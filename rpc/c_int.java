public class c_int {

    private byte[] buf = new byte[4]; // little endian    

    public int getSize() {
        return buf.length; 
    } 

    public int getValue() {
       return
            (buf[0] & 0xFF)      |   
            (buf[1] & 0xFF) << 8 | 
            (buf[2] & 0xFF) << 16| 
            (buf[3] & 0xFF) << 24; 
    }    
    public void setValue(byte[] buf) {
        this.buf = buf;
    } 

    public void setValue(int v) {
        buf = new byte[4];   
        buf[0] = (byte) (v);    
        buf[1] = (byte) (v >> 8);
        buf[2] = (byte) (v >> 16);
        buf[3] = (byte) (v >> 24);
    } 

    public byte[] toByte() {
        return buf;
    }
}
