import java.io.*;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class FTPClient {

    private static String fileDir="F://client-directory/";
    private static Scanner ctrlScanner;
    private static PrintWriter ctrlWriter;
    private static BufferedInputStream dataIs;
    private static BufferedOutputStream dataOs;
    private static Scanner dataScanner;
    private static PrintWriter dataWriter;
    private static Scanner userInputScanner;


    private static MonitorInputSteam monitorInputSteam;
    private static MonitorOutputSteam monitorOutputSteam;

    private static byte[] buff = new byte[1024];


    /**
     * @param args 格式 ipAddress port
     */
    public static void main(String[] args) {

        if (args==null||args.length != 2) {
            System.out.println("格式 ipAddress port");
            return;
        }

        initPath();


        InetAddress addr = null;
        try {
            addr = (InetAddress) Inet4Address.getByName(args[0]);//第一个参数 为 服务器 IP
        } catch (UnknownHostException e) {
            System.out.println("输入IP无法识别");
            return;
        }
        Integer port = new Integer(args[1]);//端口



        try {
            Socket ctrlSocket = new Socket(addr,port);
            ctrlScanner = new Scanner(ctrlSocket.getInputStream());
            ctrlWriter = new PrintWriter(ctrlSocket.getOutputStream(),true);
            System.out.println("控制连接已经建立在 " + ctrlSocket.getInetAddress() + " 端口: " + ctrlSocket.getPort());
            boolean flag=ctrlScanner.nextBoolean();
            if(!flag){
                //连接失败
                System.out.printf("连接失败,原因"+ctrlScanner.next());
                ctrlSocket.close();//记得关闭
                return;
            }
            Long connectionId = ctrlScanner.nextLong();//成功的话就可以直接获取前面控制连接生成的标识符
            System.out.println("成功连接服务器 此次连接ID:" + connectionId);

            Socket dataSocket = new Socket(addr,port+1);//连接服务器的数据连接
            dataIs=new BufferedInputStream(dataSocket.getInputStream());


            monitorInputSteam=new MonitorInputSteam(dataIs);

            dataOs=new BufferedOutputStream(dataSocket.getOutputStream());

            monitorOutputSteam=new MonitorOutputSteam(dataOs);

            dataScanner = new Scanner(monitorInputSteam);
            dataWriter = new PrintWriter(monitorOutputSteam,true);
            dataWriter.println(connectionId.toString());//发送ID以便服务器识别
            System.out.println("数据连接成功,客户端IP: " + dataSocket.getInetAddress() + " 端口: " + dataSocket.getPort());


            userInputScanner = new Scanner(System.in);//获取用户输入

            String inputLine = "";
            String userCommand = "";
            StringBuilder userArg;
            while (! userCommand.equals("quit")) {

                userArg = new StringBuilder();
                System.out.print("ftp> ");
                inputLine = userInputScanner.nextLine().trim();
                String[] commandStrings = inputLine.split(" ");
                if (commandStrings != null && commandStrings.length > 0 && !commandStrings[0].trim().isEmpty()) {

                    userCommand = commandStrings[0].trim();


                    for (int i = 1; i<commandStrings.length; ++i) {
                        userArg.append(commandStrings[i]);
                        userArg.append(" ");
                    }

                    switch (userCommand) {
                        case "put":
                            if (do_put(userArg.toString().trim())) {

                            } else {
                                System.out.println("出错");
                            }
                            break;
                        case "get":
                            if (do_get(userArg.toString().trim())) {

                            } else {
                                System.out.println("出错");
                            }
                            break;
                        case "list":
                            do_list();
                            break;
                        case "quit":
                            do_quit();
                            userInputScanner.close();
                            ctrlSocket.close();
                            dataSocket.close();
                            break;
                        default:
                            System.out.println("无效的命令.");
                    }
                }

            }
        } catch (ConnectException e) {
            System.out.println("无法连接服务器!");
        } catch (IOException e) {
            System.out.println("IO错误:" + e);
            e.printStackTrace();
        }
    }


    /**
     * 断开连接
     */
    private static void do_quit() {
        ctrlWriter.println("CLOSE");//请求服务器断开连接
    }

    private static void do_list() {
        ctrlWriter.println("LIST");
        System.out.println("\nFTP服务端目录:");
        String fileName = dataScanner.nextLine();
        while(!fileName.equals("$")) {
            System.out.println(fileName);
            fileName = dataScanner.nextLine();
        }
        System.out.println();
    }


    private static boolean do_get(String fileName) {
        boolean result = false;
        File outFile = new File(fileDir+fileName);
        try {

            File p=new File(outFile.getAbsolutePath());
            if(!p.exists()){
                p.mkdirs();
            }


            if(outFile.exists()) {
                outFile.delete();
            }



            outFile.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(outFile);

            ctrlWriter.println("GET " + fileName);
            long size = dataScanner.nextLong();
            long len = 0;
            int recv = 0;
            if (size > 0) {
                while (len < size) {
                   // System.out.printf("len:"+len+"  recv:"+recv+"  size:"+size+"\n");

                    recv = monitorInputSteam.read(buff,0,buff.length);
                    System.out.printf("下载速度:"+monitorInputSteam.getCurrentbps()+"bps\n");
                    len += recv;
                    fileOutputStream.write(buff,0,recv);
                    fileOutputStream.flush();
                }
            }
            System.out.printf("文件传输完毕");

            fileOutputStream.close();
            if (ctrlScanner.next().equals("OK")) {
                System.out.println("接收文件 " + fileName+" 完毕");
                result = true;
            } else {
                outFile.delete();
            }

        } catch (IOException e) {
            System.out.println("GET执行期间发生错误" +e);
        }
        return result;
    }

    private static boolean do_put(String fileName) {
        boolean result = false;
        System.out.printf(fileName+"\n");
        File inFile = new File(fileDir+fileName);
        try {
            FileInputStream fileInputStream = new FileInputStream(inFile);
            ctrlWriter.println("PUT");
            ctrlWriter.flush();
            dataWriter.println(fileName);
            dataWriter.flush();
            dataWriter.println(inFile.length());
            dataWriter.flush();
            int len=0;
            int recv = 0;
            while ((recv = fileInputStream.read(buff, 0, buff.length)) > 0) {

                len+=recv;
                monitorOutputSteam.write(buff,0,recv);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.printf("上载速度:"+monitorOutputSteam.getCurrentbps()+"bps\n");
                monitorOutputSteam.flush();

               // System.out.printf("len:"+len+"  recv:"+recv+"\n");
            }
            fileInputStream.close();
            if (ctrlScanner.next().equals("OK")) {
                result = true;
            } else {
                System.out.println("发送文件失败");
            }

        } catch (FileNotFoundException e) {
            System.out.println("文件 " + fileName + " 未找到");
        } catch (IOException e) {
            System.out.println("PUT执行出错 " + e);
        }

        return result;
    }

    private static void initPath(){
        File dir=new File(fileDir);
        if (!dir.exists()){
            dir.mkdir();
        }

    }

}
