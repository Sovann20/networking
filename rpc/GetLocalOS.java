import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.*;

public class GetLocalOS {
    
    public c_char valid;
    private String os;

    public GetLocalOS() {
        valid = new c_char(); 
    }   

    public String getOS() {
        return os;
    }

    public void execute(String IP, int PORT) throws IOException {
                               
        Socket server = new Socket(IP, PORT);
        DataOutputStream out = new DataOutputStream(server.getOutputStream());

        BufferedReader in = new BufferedReader( new InputStreamReader(server.getInputStream()));

        c_int parameterLength = new c_int();  
        parameterLength.setValue(valid.getSize());
        String stub = "GetLocalOS";
                               
        byte[] buf = new byte[100];
        byte[] paramsLength = parameterLength.toByte();
        byte[] param2 = valid.toByte();
                               
        byte[] recvBuf = new byte[1024];


        c_char stubChar = new c_char();

        for(int i = 0; i < stub.length(); i++) {
           buf[i] = (byte) stub.charAt(i); 
        }   
   
        out.write(buf, 0, 100);
        out.write(paramsLength, 0, paramsLength.length);
        out.write(param2, 0, param2.length);
       
        String osString = "";

        osString = in.readLine(); 
        //System.out.println("1: "+osString);

        this.os = osString;
        valid.setValue(true);

        in.close();
        out.close(); 
    }

}
