import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
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
	private InputStream dataIs;
	private OutputStream dataOs;
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
			dataIs=new MonitorInputSteam(dataSocket.getInputStream(),Integer.MAX_VALUE);//流入流量监控
			monitorInputSteam= (MonitorInputSteam) dataIs;
			dataOs=new MonitorOutputSteam(dataSocket.getOutputStream(),Integer.MAX_VALUE);//流出流量监控
			monitorOutputSteam= (MonitorOutputSteam) dataOs;
			dataScanner = new Scanner(dataIs);
			dataWriter = new PrintWriter(dataOs,true);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Problem getting input and/or outputsreams for data and/or control sockets:" + e);
		}
	}
	
	
	//entry point for the control socket
	@Override
	public void run() {
		
		String cmd = controlScanner.next();
		
		while (!cmd.equals("CLOSE")) {
			System.out.println("Recieved command: " + cmd);
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
				System.out.println("Invalid socket control message received");
				controlWriter.println("INVALID");
				break;				
			}
			cmd = controlScanner.next();

		}
		try {
			System.out.println("Session ended from " + controlSocket.getInetAddress() +" port " + controlSocket.getPort());
			controlSocket.close();
			dataSocket.close();
		} catch (IOException e) {
			System.out.println("Problem closing control and/or data socket " + e);
		}
	}	

	
	/**
	 * Sends an existing file to a client
	 */
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
					System.out.println("上传速度:"+monitorOutputSteam.getCurrentbps()+"bps\n");
					dataOs.write(buff,0,recv);
				}
				dataOs.flush();
				fileStream.close();
				System.out.println("sent file " + fname);
				result = true;
			} catch (IOException e) {
				System.out.println("Error receiving file." + e);
			}			
		} else {
			dataWriter.println(0);
			System.out.println("File " + inFile + " does not exist.");
		}
		return result;
	}
	
	/**
	 * Client is putting a file on the server
	 * @return
	 */
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
				while(len + recv < size) {
					System.out.printf("len:"+len+"  recv:"+recv+"  size:"+size+"\n");
					//System.out.printf("下载速度:"+monitorInputSteam.getCurrentbps()+"bps\n");


					recv = monitorInputSteam.read(buff, 0, buff.length);
					if (recv==-1){
						break;
					}
					fileStream.write(buff,0,recv);
					len += recv;
				}
			}
			fileStream.flush();
			
			fileStream.close();
			result = true;			
			
		} catch (IOException e) {
			System.out.println("Problem creating output file stream: " + e);
		}
		
		return result;		
	}
	
	/**
	 * List the contents of the directory and send them to the client
	 */
	public void do_list() {
		Path currentPath = Paths.get(FTPServer.getFileFullPath(""));
		File[] dirList = currentPath.toFile().listFiles();
		for (File f : dirList) {
			dataWriter.println(f.getName());
		}
		dataWriter.println("$");
	}




}
