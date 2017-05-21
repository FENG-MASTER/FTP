import java.io.IOException;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;


/**
 * 命令传输线程+
 */
public class ControlLinkHandler implements Runnable {
	
	private static Random random;//随机器
	
	private ServerSocket controlServerSocket;//控制socket
	private PrintWriter writer;
	
	static {
		random = new Random();
	}


	/**
	 * @param port 初始化端口号
	 * @throws IOException
	 */
	public ControlLinkHandler(int port) throws IOException {
		controlServerSocket = new ServerSocket(port,10,Inet4Address.getLocalHost());
		//本地启动一个socket,第二个参数是最大正在连接数(并不是最大连接数)
		System.out.println("控制连接已经在 " + controlServerSocket.getInetAddress() +
				" 端口: " + controlServerSocket.getLocalPort()+"开启");
	}	
		
	@Override
	public void run() {
		//无限循环去接收客户端的消息
		while(true) {
			Socket socket;
			try {
				socket = controlServerSocket.accept();//接收客户端连接,该方法会堵塞
				System.out.println("控制连接 接收到 来自 客户端"
						+ socket.getInetAddress() + " 端口: " + socket.getPort()+"的连接请求");
				socket.setSoTimeout(5000); //read timeout

				writer = new PrintWriter(socket.getOutputStream(),true);

				Object[] objects=new Object[2];
				if(IPManger.canConnect(socket,objects)){
					//如果可以连接
					Long connectionId = random.nextLong();//生成客户端ID识别码
					writer.println(true);
					writer.println(connectionId.toString());
					IPManger.connetingMap.put(connectionId, socket);

				}else {
					writer.println(false);
					writer.println(objects[0]);//返回给客户端的错误信息
					System.out.printf(objects[1].toString());
					socket.close();
				}

				
			} catch (IOException e) {
				throw new IllegalStateException("客户端连接时出现IO错误:" + e);
			}
		}
	}
}
