import java.util.Scanner;

/**
 * Created by qianzise on 2017/5/23.
 */
public class ServerUI implements Runnable{
    @Override
    public void run() {

        Scanner scanner=new Scanner(System.in);

        String comand=scanner.next().trim().toUpperCase();
        while (true){
            switch (comand){
                case "BAN":
                    String ip=scanner.next();
                    IPManger.ban(ip);
                    System.out.println("添加黑名单成功");
                    break;
                case "WHITE":
                    String ip1=scanner.next();
                    IPManger.white(ip1);
                    System.out.println("添加白名单成功");
                    break;
                case "OPEN":
                    String what=scanner.next().trim().toUpperCase();

                    if (what.equals("BAN")){
                        IPManger.banOpen=true;
                        IPManger.whiteOpen=false;
                        System.out.println("服务器黑名单 开启 ,白名单 关闭 ");
                    }else if (what.equals("WHITE")){
                        IPManger.banOpen=false;
                        IPManger.whiteOpen=true;
                        System.out.println("服务器黑名单 关闭 ,白名单 开启 ");
                    }else {
                        System.out.println("输入有误");
                    }

                    break;
                case "CLOSE":
                    String what1=scanner.next().trim().toUpperCase();

                    if (what1.equals("BAN")){
                        IPManger.banOpen=false;
                        System.out.println("服务器黑名单 关闭");
                    }else if (what1.equals("WHITE")){
                        IPManger.whiteOpen=false;
                        System.out.println("服务器白名单 关闭 ");
                    }else {
                        System.out.println("输入有误");
                    }

                    break;
                default:
                    System.out.println("无效指令,请重新输入.");
                    break;
            }
            comand=scanner.next().trim().toUpperCase();
        }
    }
}
