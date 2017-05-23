
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MonitorOutputSteam extends FilterOutputStream {


    private long timestamp;
    private int maxbps;
    private int currentbps;
    private int bytesread;


    public MonitorOutputSteam(OutputStream out, int maxbps){
        super(out);
        this.maxbps = maxbps;
        this.currentbps = 0;
        this.bytesread = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public MonitorOutputSteam(OutputStream out){
        this(out,Integer.MAX_VALUE);
    }

    @Override
    public void write(int b) throws IOException {
        synchronized (out){
            int avaliable = check();
            if(avaliable == 0){
                waitForAvailable();
                avaliable = check();
            }

            out.write(b);
            update(1);
        }


    }



    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized (out){
            int avaliable = check();
            if(avaliable == 0){
                waitForAvailable();
                avaliable = check();
            }

            out.write(b,off,len);
            update(len);
        }


    }

    @Override
    public void write(byte[] b) throws IOException{
        this.write(b,0,b.length);

    }

    private int check(){
        long now = System.currentTimeMillis();
        if(now - timestamp >= 1000){
            timestamp = now;
            //currentbps = bytesread;
            bytesread = 0;
            return maxbps;
        }else{
            return maxbps - bytesread;
        }
    }

    private void waitForAvailable(){
        long time = System.currentTimeMillis() - timestamp;
        boolean isInterrupted = false;
        while(time < 1000){
            try{
                Thread.sleep(1000 - time);
            }catch(InterruptedException e){
                isInterrupted = true;
            }
            time = System.currentTimeMillis() - timestamp;
        }
        if(isInterrupted)
            Thread.currentThread().interrupt();
        return;

    }

    private void update(int n){
        bytesread += n;
        if (System.currentTimeMillis()!=timestamp){
            currentbps= (int) (bytesread/(System.currentTimeMillis()-timestamp));
        }

    }

    public int getCurrentbps(){
        return currentbps;
    }
}
