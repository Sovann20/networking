import java.net.*;
import java.io.*;
import java.nio.*;

public class Test {

    private static final String IP = "127.0.0.1";
    private static final int PORT = 4726;

    public static void main(String args[]) throws IOException {
    
        GetLocalTime gt = new GetLocalTime();
        gt.valid.setValue(false);
        gt.execute(IP, PORT);
        int t = gt.time.getValue();
        char v = gt.valid.getValue();

        System.out.println("Valid: "+v+"\nTime: "+t+"\n");
    
        GetLocalOS go = new GetLocalOS();
        go.valid.setValue(true);
        go.execute(IP, PORT);
        String os = go.getOS();
        v = go.valid.getValue();

        System.out.println("Valid: "+v+"\nTime: "+os+"\n");

    }
}
