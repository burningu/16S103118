import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;

public class Pthread extends Thread{
	
	private Socket socket;
	private BufferedReader in;
    private PrintWriter out;
	
    public Pthread(Socket socket) {
		this.socket = socket;
	}
    
	public void run() {
		
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	        out = new PrintWriter(socket.getOutputStream(), false);
	        System.out.println(Config.server_name + ": " + Config.info_msg +" 监测到" + socket.getInetAddress().getHostAddress() +" 正在尝试连接到服务器");
	        out.println(Config.server_name + ": " + Config.user_msg);
	        out.flush();
	        String temp = "";
	        while (!temp.equals("CONNECT") && !temp.equals("QUIT")) {
				temp = in.readLine();
				if (temp == null) 
					temp = "";
				if (temp.equals("CONNECT")) {
					System.out.println(Config.server_name + ": " + Config.ok_msg +" 检测到" + socket.getInetAddress().getHostAddress() +" 向服务器发送了一个握手消息，返回确认消息");
					out.println("ACCEPT");
			        out.flush();
				}
			}
	        //服务端已经发送确认信息，等待客户端进一步的消息
	        while (!temp.equals("QUIT")) {
	        	temp = in.readLine();
				if (temp == null) 
					temp = "";
				if (temp.substring(0, 3).equals("ADD")) {
					boolean flag = false;
					String[] cmd = temp.split(" ");
					String filename = cmd[1];
					if (filename != null) {
						String filehash = cmd[2];
						if (filehash != null) {
							String filesize = cmd[3];
							if (filesize != null) {
								String peeraddr = socket.getInetAddress().getHostAddress();
								int status = Napster.sqlite.sqlite_insert(filename, filehash, filesize, peeraddr);
								if (status == 0) {
									System.out.println(Config.server_name + ": " +Config.error_msg +" 添加文件到数据库出错");
									out.println("ERROR 添加文件到数据库出错");
									out.flush();
								} else {
									System.out.println(Config.server_name + ": " +Config.info_msg + " 客户端" + peeraddr + " 向服务器添加了文件" + filename + "[hash值  : " + filehash + "][大小:" +filesize + "]" );
									out.println("OK");
									out.flush();
								}
							} else {
								flag = true;
							}
						} else {
							flag = true;
						}
					} else {
						flag = true;
					}
					if (flag) {
						System.out.println(Config.server_name + ": " +Config.error_msg + "添加文件失败，传入参数的格式错误");
						out.println("ERROR 添加文件失败，传入参数的格式错误");
					}
				} else if (temp.length() > 6 && temp.substring(0, 6).equals("DELETE")) {
					boolean flag = false;
					String[] cmd = temp.split(" ");
					String filename = cmd[1];
					if (filename != null) {
						String filehash = cmd[2];
						if (filehash != null) {
							String peeraddr = socket.getInetAddress().getHostAddress();
							int status = Napster.sqlite.sqlite_delete(filename, filehash, peeraddr);
							if (status == 0) {
								System.out.println(Config.server_name + ": " +Config.error_msg +" 删除文件出错");
								out.println("ERROR 从服务器删除文件出错");
								out.flush();
							} else {
								out.println(Config.server_name + ": " +Config.ok_msg + " 客户端" + peeraddr + " 向服务器删除了文件'" + filename + "'('" + filehash + "')");
								out.flush();
							}
						} else {
							flag = true;
						}
					} else {
						flag = true;
					}
					if (flag) {
						System.out.println(Config.server_name + ": " +Config.error_msg + "添加文件失败，传入参数的格式错误");
						out.println("ERROR 删除文件失败，传入参数的格式错误");
					}
				} else if (temp.equals("LIST")) {
					HashSet<String[]> res = Napster.sqlite.sqlite_list(socket.getInetAddress().getHostAddress());
					if (res.size() == 0) {
						System.out.println(Config.server_name + ": " +Config.error_msg +" 未能获得所有记录，数据库错误");
						out.println("ERROR 未能获得所有记录，服务端数据库错误");
						out.flush();
					} else {
						for (String[] fileinfo : res) {
							out.println(fileinfo[0] + " " + fileinfo[1]);
							out.flush();
						}
					}
					out.println("OK");
					out.flush();
				} else if (temp.equals("QUIT")) {
					continue;
				} else if (temp.length() > 7 && temp.substring(0, 7).equals("REQUEST")) {
					String[] cmd = temp.split(" ");
					String filename = cmd[1];
					if (filename != null) {
						String[] res = Napster.sqlite.sqlite_select(filename, socket.getInetAddress().getHostAddress());
						if (res == null) {
							System.out.println(Config.server_name + ": " +Config.error_msg + "未能成功获取文件信息，数据库错误");
							out.println("ERROR 未能成功获取文件信息，数据库错误");
							out.flush();
						} else {
							out.println(res[0] + " " + res[1]);
							out.flush();
						}
						out.println("OK");
						out.flush();
					} else {
						out.println("ERROR 没能成功获得请求的文件名");
						out.flush();
					}
				} else {
					out.println("ERROR 参数错误");
					out.flush();
				}
			}
	        out.println("GOODBYE");
	        out.flush();
	        int status = Napster.sqlite.sqlite_delete(socket.getInetAddress().getHostAddress());
	        String ip = socket.getInetAddress().getHostAddress();
	        if (status == 0) {
				System.out.println("客户端" + socket.getInetAddress().getHostAddress() + "剔除出错");
			}
	        socket.close();
	        System.out.println(Config.server_name + ": " + Config.ok_msg + "客户端" + ip + "已从服务器注销登录");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
