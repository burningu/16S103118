import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

//用于计算 hash 值
import org.apache.commons.codec.digest.DigestUtils;

public class Nap {
    public static void error_handler(String err) {
        System.out.println("\033[1;31m[错误] >>\033[0m " + err.substring(6));
        System.exit(-1);
    }

    // Main method
    public static void main(String[] args) {
        try {
            System.out.println("Nap 客户端");
            Socket socket;
            BufferedReader in;
            PrintWriter out;
			// 初始化用于接收用户输入的 stdin
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            String server;//P2P 服务器 IP
            int port;//P2P 服务器端口
            String path;//本地 P2P 工作目录
            String request = "";
            String[] reqArray;
            String response;
            String[] respArray;
			//获取几个必要信息
            System.out.print("服务器的 IP 地址 >> ");
            server = stdin.readLine();
            System.out.print("服务器的端口号 >> ");
            port = Integer.parseInt(stdin.readLine());
            System.out.print("本机的工作目录 >> ");
            path = stdin.readLine();
            Global.path = path;
            socket = new Socket(server, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));//服务器返回的消息
            out = new PrintWriter(socket.getOutputStream(), false);//发送给服务器的消息
            // 打印服务器返回的消息
            System.out.println(in.readLine());
			// 发送握手消息"CONNECT"，开始握手
            out.println("CONNECT");
            out.flush();
            response = in.readLine();
			// 接收确认信息
            if (!response.equals("ACCEPT")) {
                System.out.println("\033[1;31m[错误] >>\033[0m 向服务端发送的握手信息未能接收到正确的确认包");
                System.exit(-1);
            } else {
                System.out.println("\033[1;32m[成功] >>\033[0m 成功连接到 Napd 服务器" + server + ":" + port);
            }
            File folder = new File(path);
            File[] files = folder.listFiles();
            FileInputStream f_stream;
            String filename;
            String filehash;
            String filesize;
            System.out.println("[信息] 正在为工作目录 " + path + " 建立文件索引...");
            int index_total = 0;//记录文件夹下成功上传的文件数
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    filename = files[i].getName();
                    //读文件
                    f_stream = new FileInputStream(files[i]);
                    filehash = DigestUtils.md5Hex(f_stream);
                    f_stream.close();
                    filesize = String.valueOf(files[i].length());
                    out.println("ADD " + filename + " " + filehash + " " + filesize);
                    out.flush();
                    response = in.readLine();
                    if (!response.equals("OK"))
                        error_handler(response);
                    else {
                        System.out.print(". ");
                        index_total++;
                    }
                }
            }
            System.out.println("\n\033[1;32m[ 成 功 ] >>\033[0m 成 功 添 加 " + index_total + " 个文件信息到服务器");
			// 开启文件服务器线程
            Runnable run = new peer_server();
            Thread thread = new Thread(run);
            thread.start();
            System.out.println("[信息] 等待用户输入");
            do {
                System.out.print(">> ");
                request = stdin.readLine();
                reqArray = request.split(" ");
                if (request.equals("list")) {
                    System.out.println("[信息] 正在向服务器请求文件列表...");
					// 发送 LIST 命令
                    out.println("LIST");
                    out.flush();
                    int list_total = 0;
                    response = in.readLine();
                    respArray = response.split(" ");
                    while ((!respArray[0].equals("OK")) && (!respArray[0].equals("ERROR"))) {
                        list_total++;
                        System.out.println(String.format("[%2d] : %20s [文件大小:%10s]", new Object[] { new Integer(list_total), respArray[0], respArray[1] }));
                        response = in.readLine();
                        respArray = response.split(" ");
                    }
                    System.out.println("[信息] 一共获取到 " + list_total + " 个文件 ");
                    if (!response.equals("OK"))
                        error_handler(response);
                } else if (reqArray[0].equals("request")) {
                    try {
                        if (!reqArray[1].isEmpty()) {
							//发送 REQUEST
                            out.println("REQUEST " + reqArray[1]);
                            out.flush();
                            response = in.readLine();
                            respArray = response.split(" ");
                            if (respArray[0].equals("OK"))
                                System.out.println("\033[1;31m[错误] >>\033[0m 在服务器上并未找到文件 '" + reqArray[1]);
                            while ((!respArray[0].equals("OK")) && (!respArray[0].equals("ERROR"))) {
								//respArray 格式：peer 的 IP+文件大小
                                @SuppressWarnings("resource")
								Socket comSocket = new Socket(respArray[0], 7701);
                                String comResponse;
                                BufferedReader comIn = new BufferedReader(new InputStreamReader(comSocket.getInputStream()));
                                PrintWriter comOut = new PrintWriter(comSocket.getOutputStream(), false);
								//验证身份
                                comOut.println("HELLO");
                                comOut.flush();
                                comResponse = comIn.readLine();
								//确认
                                if (!comResponse.equals("ACCEPT")) {
                                    System.out.println("\033[1;31m[ 错误]>>\033[0 m 客户端握手消息验证失败 ");
                                    System.exit(-1);
                                }
                                Socket fileSocket = new Socket(respArray[0], 7702);
                                comOut.println("GET " + reqArray[1]);
                                comOut.flush();
                                InputStream fileIn = fileSocket.getInputStream();
                                File f = new File(path + File.separator + "recv");
                                if (!f.exists()) {
                                    f.mkdirs();
                                }
                                BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(path + File.separator + "recv" + File.separator + reqArray[1]));
                                int bytesRead, current = 0;
                                byte[] buffer = new byte[Integer.parseInt(respArray[1])];
                                bytesRead = fileIn.read(buffer, 0, buffer.length);
                                current = bytesRead;
                                System.out.println("[信息] 开始传输文件...");
                                do {
                                    System.out.print(". ");
                                    bytesRead = fileIn.read(buffer, current, (buffer.length - current));
                                    if (bytesRead >= 0)
                                        current += bytesRead;
                                } while (bytesRead > -1 && buffer.length != current);
                                fileOut.write(buffer, 0, current);
                                fileOut.flush();
                                System.out.println("\n\033[1;32m[ 成功]>>\033[0 m 文件传输成功 ");
                                fileIn.close();
                                fileOut.close();
                                fileSocket.close();
                                respArray[0] = "OK";
                                response = in.readLine();
                                respArray = response.split(" ");
                            }
                            if (!respArray[0].equals("OK"))
                                error_handler(response);
                        }
                    } catch (Exception e) {
                        System.out.println("\033[1;31m[错误] >>\033[0m " + e);
                    }
                }
            } while (!request.equals("quit"));
            out.println("QUIT");
            out.flush();
            response = in.readLine();
            if (!response.equals("GOODBYE")) {
                System.out.println("\033[1;31m[错误] >>\033[0m 程序未正常退出： " + response);
                System.exit(-1);
            } else {
                System.out.println("\033[1;32m[成功] >>\033[0m 成功关闭连接");
            }
            in.close();
            out.close();
            socket.close();
        } catch (Exception e) {
            System.out.println("\033[1;31m[错误] >>\033[0m " + e);
        }
    }
}
