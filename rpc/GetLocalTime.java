import java.net.*;
import java.io.*;
import java.nio.*;

public class GetLocalTime {

    public c_int time;
    public c_char valid;

    public GetLocalTime() {
        time = new c_int();
        valid = new c_char(); 
    }

    public void execute(String IP, int PORT) throws IOException {
       
        Socket server = new Socket(IP, PORT);
        DataOutputStream out = new DataOutputStream(server.getOutputStream());
        DataInputStream in = new DataInputStream(server.getInputStream());

        c_int parameterLength = new c_int();  
        parameterLength.setValue(time.getSize() + valid.getSize());
        String stub = "GetLocalTime";
        
        byte[] buf = new byte[100];
        byte[] paramsLength = parameterLength.toByte();
        byte[] param1 = time.toByte();
        byte[] param2 = valid.toByte();
    
        byte[] recvBuf = new byte[parameterLength.getValue()];

        c_char stubChar = new c_char();

        for(int i = 0; i < stub.length(); i++) {
           buf[i] = (byte) stub.charAt(i); 
        }
   
        out.write(buf, 0, 100);
        out.write(paramsLength, 0, paramsLength.length);
        out.write(param1, 0, param1.length);
        out.write(param2, 0, param2.length);
        
        in.read(recvBuf);
        
        byte[] recvParam1 = new byte[4];

        for(int i = 0; i < 4; i++) { 
            recvParam1[i] = recvBuf[i];
        }
       
        time.setValue(recvParam1);
        valid.setValue(true);
        
        in.close();
        out.close(); 
    }

}
