import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

class peer_server implements Runnable {
    // 实现一个基本的文件服务器
    @SuppressWarnings("resource")
	public void run() {
        try {
            // 预定义
            String path = Global.path;
            // 建立用于接收服务器消息的 ServerSocket
            ServerSocket comServSock = new ServerSocket(7701);
            // 建立用于接收另一个 peer 消息、传输文件的 ServerSocket
            ServerSocket fileServSock = new ServerSocket(7702);
            while (true) {
                Socket socket = comServSock.accept();
                //创建输入输出流
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), false);
                String response = "";
                String[] respArray;
				// 循环监听连接请求，直到接收到"HELLO"或者"QUIT"握手消息
                while (!response.equals("HELLO") && !response.equals("QUIT")) {
                    response = in.readLine();
					// 如果接收到的握手消息是 OPEN，回复确认消息 HELLO
                    if (response.equals("HELLO")) {
                        out.println("ACCEPT");
                        out.flush();
                    }
                }
				// 循环监听连接请求，直到收到 QUIT 握手消息
                while (!response.equals("QUIT")) {
                    response = in.readLine();
                    //请求参数分段处理
                    respArray = response.split(" ");
					// syntax: GET [filename]
                    if (respArray[0].equals("GET")) {
                        try {
							// 请求的文件名不为空
                            if (!respArray[1].isEmpty()) {
								// 新建一个用于文件传输的 socket
                                Socket fileSocket = fileServSock.accept();
                                File peerfile = new File(path + File.separator + respArray[1]);
                                byte[] buffer = new byte[(int) peerfile.length()];
                                BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(peerfile));
                                fileIn.read(buffer, 0, buffer.length);
                                BufferedOutputStream fileOut = new BufferedOutputStream(fileSocket.getOutputStream());
                                fileOut.write(buffer, 0, buffer.length);
                                fileOut.flush();
                                fileIn.close();
                                fileOut.close();
                                fileSocket.close();
                                out.println("OK");
                                out.flush();
                            }
                        } catch (Exception e) {
                            out.print("ERROR " + e);
                            out.flush();
                        }
                    } else if (response.equals("CLOSE")) {
                        continue;
                    }
                }
                out.print("GOODBYE");
                out.flush();
                socket.close();
            }
        } catch (Exception e) {
            System.out.println("\033[1;31m[错误] >>\033[0m " + e);
            System.exit(-1);
        }
    }
}
