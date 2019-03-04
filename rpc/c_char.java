public class c_char {

    private byte[] buf = new byte[1];  // little endian    

    public int getSize() {
        return buf.length; 
    } 

    public char getValue() {
       return (char) (buf[0]);
    }

    public void setValue(byte[] buf) {
        this.buf = buf;
    } 

    public void setValue(char v) {
        buf[0] = (byte) (v); 
    } 
    
    public void setValue(boolean val) {

        if(val) {
            buf[0] = (byte) '1';
        } else {
            buf[0] = (byte) '0';
        }
          
    } 

    public byte[] toByte() {
        return buf;
    }
}
