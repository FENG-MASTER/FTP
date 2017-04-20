import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

/**
 * 数据传输线程
 */
public class DataLinkHandler implements Runnable {
	
	private ServerSocket dataServerSocket;
	private Scanner scanner;

	/**
	 * @param port 端口号
	 * @throws IOException
	 */
	public DataLinkHandler(int port) throws  IOException {
		dataServerSocket = new ServerSocket(port,10,Inet4Address.getLocalHost());
		System.out.println("数据连接已经在 " + dataServerSocket.getInetAddress() +
				" 端口: " + dataServerSocket.getLocalPort()+"开启");
	}	
		
	@Override
	public void run() {
		//无线循环
		while(true) {
			Socket dataSocket = null;
			Socket ctrlSocket = null;
			try {
				dataSocket = dataServerSocket.accept();
				System.out.println("数据连接 接收到 来自 客户端 " + dataSocket.getInetAddress() +
						" 端口: " + dataSocket.getPort()+"的连接请求");
				dataSocket.setSoTimeout(5000);
				scanner = new Scanner(dataSocket.getInputStream());
				Long connectionId = scanner.nextLong();
				
				ctrlSocket = IPManger.connetingMap.get(connectionId);//根据前面控制连接返回的ID去获取控制socket
				
				if (ctrlSocket == null) {
					System.out.println("客户端并未连接到服务端的控制连接 " +
							dataSocket.getInetAddress() + " 端口: " + dataSocket.getPort());
					dataSocket.close();
				} else {
					System.out.println("客户端数据连接成功 " +
							dataSocket.getInetAddress() + " 端口 " + dataSocket.getPort());
					IPManger.connetingMap.remove(connectionId);//删除控制连接
					IPManger.conSockets.add(dataSocket);
					new Thread(new ServerSession(ctrlSocket,dataSocket)).start();//开启用户服务线程
				}
			} catch (SocketTimeoutException e) {
				System.out.println("数据连接超时,关闭连接");
				try {
					if (dataSocket != null && !dataSocket.isClosed())					
						dataSocket.close();
					} catch (IOException e1) {
						throw new IllegalStateException("关闭连接出错 " + e1);
					}
			} catch (IOException e) {
				throw new IllegalStateException("数据连接出错: " + e);
			} 
		}
	}
}
