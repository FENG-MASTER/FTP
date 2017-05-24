import com.sun.xml.internal.bind.marshaller.DataWriter;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class ServerSession implements Runnable {
	
	private Socket controlSocket;
	private Socket dataSocket;
	
	private Scanner controlScanner;
	private PrintWriter controlWriter;
	
	
	private Scanner dataScanner;
	private BufferedInputStream dataIs;
	private BufferedOutputStream dataOs;
	private PrintWriter dataWriter;
	private byte[] buff = new byte[1024];

	private MonitorOutputSteam monitorOutputSteam;
	private MonitorInputSteam monitorInputSteam;


	
	//store socket and get input stream scanner
	public ServerSession(Socket controlSocket, Socket dataSocket) {
		super();
		this.controlSocket = controlSocket;		
		this.dataSocket = dataSocket;		
		try {
			this.controlSocket.setSoTimeout(0);
			this.dataSocket.setSoTimeout(0);
			controlScanner = new Scanner(controlSocket.getInputStream());
			controlWriter = new PrintWriter(controlSocket.getOutputStream(),true);
			dataIs=new BufferedInputStream(dataSocket.getInputStream());
			monitorInputSteam= new MonitorInputSteam(dataIs);
			dataOs=new BufferedOutputStream(dataSocket.getOutputStream());
			monitorOutputSteam=new MonitorOutputSteam(dataOs);
			dataScanner = new Scanner(monitorInputSteam);
			dataWriter = new PrintWriter(monitorOutputSteam,true);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("sockets流初始化失败" + e);
		}
	}
	
	
	//entry point for the control socket
	@Override
	public void run() {

		String cmd = controlScanner.next();
		
		while (!cmd.equals("CLOSE")) {
			System.out.println("接收到命令 : " + cmd);
			switch (cmd) {
			case "LIST":
				do_list();
				break;
			case "GET":
				if (do_get()) {
					controlWriter.println("OK");
				} else {
					controlWriter.println("ERROR");
				}
				break;
			case "PUT": 
				if (do_put()) {
					controlWriter.println("OK");
				} else {
					controlWriter.println("ERROR");
				}
				break;
			default:
				System.out.println("收到无效的命令");
				controlWriter.println("INVALID");
				break;				
			}
			cmd = controlScanner.next();

		}
		try {
			System.out.println("会话结束: " + controlSocket.getInetAddress() +" port " + controlSocket.getPort());
			controlSocket.close();
			dataSocket.close();
		} catch (IOException e) {
			System.out.println("关闭socket出错" + e);
		}
	}	

	

	public boolean do_get() {		
		boolean result = false;
		String fname = controlScanner.nextLine().trim();

		File inFile = new File(FTPServer.getFileFullPath(fname));
		if (inFile.exists()) {			
			InputStream fileStream;			
			try {
				fileStream = new FileInputStream(inFile);
				dataWriter.println(inFile.length());
				int recv;				
				while ((recv = fileStream.read(buff, 0, buff.length)) > 0) {

					monitorOutputSteam.write(buff,0,recv);
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					//System.out.printf("len:"+inFile.length()+"  recv:"+recv+"\n");
					System.out.println("上传速度:"+monitorOutputSteam.getCurrentbps()+"bps");
					monitorOutputSteam.flush();

				}
				monitorOutputSteam.flush();
				fileStream.close();
				System.out.println("传输文件" + fname+" 成功");
				result = true;
			} catch (IOException e) {
				System.out.println("传输文件出错" + e);
			}			
		} else {
			dataWriter.println(0);
			System.out.println("文件 " + inFile + " 未找到");
		}
		return result;
	}
	

	public boolean do_put() {
		
		boolean result = false;
		String fileName = dataScanner.nextLine();
		try {
			System.out.printf(fileName+"\n");
			//set up the output file
			File outFile = new File(FTPServer.getFileFullPath(fileName));
			if (outFile.exists())
				outFile.delete();
			outFile.createNewFile();			
			OutputStream fileStream = new FileOutputStream(outFile);
			
			//read and write the file data
			long len = 0;
			long size = Long.parseLong(dataScanner.nextLine());
			int recv = 0;
			if (size > 0) {
				while(len < size) {
					recv = monitorInputSteam.read(buff,0,buff.length);
					fileStream.write(buff,0,recv);
					System.out.printf("下载速度:"+monitorInputSteam.getCurrentbps()+"bps\n");
					fileStream.flush();
					len += recv;

					//System.out.printf("len:"+len+"  recv:"+recv+"  size:"+size+"\n");
				}
			}
			System.out.printf("文件传输完毕");
			fileStream.flush();
			
			fileStream.close();
			result = true;			
			
		} catch (IOException e) {
			System.out.println("创建文件失败 " + e);
		}
		
		return result;		
	}
	

	public void do_list() {
		listFile(FTPServer.getFileFullPath(""),0);
		dataWriter.println("$");
	}


	private void listFile(String dir,int i){
		Path currentPath = Paths.get(dir);
		File[] dirList = currentPath.toFile().listFiles();
		for (File f : dirList) {
			if (f.isDirectory()){
				dataWriter.println(f.getName());
				listFile(f.getAbsolutePath(),i+1);
			}else {
				for (int ii=0;ii<i;ii++){
					dataWriter.printf("  ");
				}
				dataWriter.println(f.getName());
			}
		}

	}




}
