import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by qianzise on 2017/5/4 0004.
 */
public class MonitorInputSteam extends FilterInputStream {


    private long timestamp;
    private int maxbps;
    private int currentbps;
    private int bytesread;


    public MonitorInputSteam(InputStream in, int maxbps){
        super(in);
        this.maxbps = maxbps;
        this.currentbps = 0;
        this.bytesread = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public MonitorInputSteam(InputStream in){
        this(in,Integer.MAX_VALUE);
    }

    @Override
    public int read() throws IOException {
        synchronized(in){
            int avaliable = check();
            if(avaliable == 0){
                waitForAvailable();
                avaliable = check();
            }
            int value = in.read();
            update(1);
            return value;
        }
    }
    @Override
    public int read(byte[] b) throws IOException{
        return read(b, 0, b.length);

    }
    @Override
        public int read(byte[] b, int off, int len) throws IOException{
        synchronized(in){
            int avaliable = check();
            if(avaliable == 0){
                waitForAvailable();
                avaliable = check();
            }
            int n = in.read(b, off, len);
            update(n);
            return n;
        }
    }

    private int check(){
        long now = System.currentTimeMillis();
        if(now - timestamp >= 1000){
            timestamp = now;
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
