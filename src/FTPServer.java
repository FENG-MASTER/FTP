import java.io.File;
import java.io.IOException;
import java.util.Scanner;


public class FTPServer {

	private static String fileDir="F://server-directory/";

	/**
	 * 入口
	 * @param args
	 */
	public static void main(String[] args) {

		initPath();
		IPManger.init();

		try {
			//参考FTP的模型,启动两个线程,一个数据传输,一个命令传输
			new Thread(new ControlLinkHandler(25060)).start();
			new Thread(new DataLinkHandler(25061)).start();
			new Thread(new ServerUI()).start();
		} catch (IOException e) {
			throw new IllegalStateException("初始化FTP服务器失败" + e);
		}


	}






	private static void initPath(){
		File dir=new File(fileDir);
		if (!dir.exists()){
			dir.mkdir();
		}

	}

	public static String getFileFullPath(String fileName){
		return fileDir+fileName;
	}
	
}
