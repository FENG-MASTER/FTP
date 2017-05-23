import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * IP控制器,加入白名单,黑名单功能
 * 白名单黑名单只能开启一个
 */
public class IPManger {

    public static int maxLinkNum=3;

    public static boolean banOpen=false;
    public static boolean whiteOpen=false;

    public static ConcurrentHashMap<Long, Socket> connetingMap = new ConcurrentHashMap<Long,Socket>();
    public static CopyOnWriteArrayList<Socket> conSockets=new CopyOnWriteArrayList<>();

    private static List<InetAddress> banList=new ArrayList<>();
    private static List<InetAddress> whiteList=new ArrayList<>();

    private static String banListFileName="BanList.txt";
    private static String whiteListFileName="WhiteList.txt";

    public static void init(){
        initList(banListFileName,banList);
        initList(whiteListFileName,whiteList);
    }

    private static void  initList(String fileName,List<InetAddress> list){
        File dir=new File(getConfigureDir());
        if (!dir.exists()){
            dir.mkdir();
        }
        File file=new File(getConfigureDir()+"/"+fileName);
        if(file.exists()){
            //读取黑名单
            Scanner scanner = null;
            try {
                scanner=new Scanner(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            while (scanner.hasNext()){

                try {
                    list.add(Inet4Address.getByName(scanner.next()));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            }

        }else {

            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean canConnect(Socket socket,Object[] obj){
        if(conSockets.size()>=maxLinkNum){
            obj[0]="超出最大连接数";
            obj[1]="超出最大连接数";
            return false;
        }else {
            if(banOpen&&isBan(socket)){
                obj[0]="您已经被服务器拉入黑名单,无法连接";
                obj[1]="有黑名单中的IP试图连接服务器";
                return false;
            }

            if(whiteOpen){
                if(isWhite(socket)){
                    return true;
                }else {
                    //不在白名单,无法连接
                    obj[0]="服务器已经开启白名单模式,您的IP不在白名单中,无法连接";
                    obj[1]="有非白名单中的IP试图连接服务器";

                    return false;
                }
            }

           return true;

        }
    }

    public static boolean isBan(Socket socket){
        InetAddress socketAddr=socket.getInetAddress();
        for (InetAddress addr:banList) {
            if (addr.equals(socketAddr)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWhite(Socket socket){
        InetAddress socketAddr=socket.getInetAddress();
        for (InetAddress addr:whiteList) {
            if (addr.equals(socketAddr)) {
                return true;
            }
        }
        return false;
    }

    public static void ban(String ip){
        File file=new File(getConfigureDir()+"/"+banListFileName);

        appendList(file,ip);

        init();
    }

    public static void white(String ip){
        File file=new File(getConfigureDir()+"/"+whiteListFileName);


        appendList(file,ip);

        init();
    }

    private  static void appendList(File file,String s){
        try {
            FileWriter writer=new FileWriter(file,true);
            writer.write(s);
            writer.write("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String getConfigureDir(){
       return FTPServer.getFileFullPath("conf");
    }
}
