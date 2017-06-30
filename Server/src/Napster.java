import java.net.ServerSocket;
import java.net.Socket;

public class Napster {
	
	public static Sqlite sqlite = new Sqlite();
	
	public static void main(String[] args) {
		
		System.out.println(Config.server_name + ": " + Config.info_msg +" 正在初始化" + Config.server_name + "...");
		System.out.println(Config.server_name + ": " +Config.info_msg + "初始化成功");
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(Config.default_port);
			while (true) {
				Socket socket = serverSocket.accept();
				Pthread pthread = new Pthread(socket);
				pthread.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
