
import java.util.StringTokenizer;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class TestCIDR {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String cidr = "201.6/16";
        int index = cidr.indexOf('/');
        if (index != -1) {
            String ip = cidr.substring(0, index);
            getIP(ip);
            int size = Integer.parseInt(cidr.substring(++index));
            getMask(size);
        }
    }
    
    private static byte[] getIP(String ip) {
        byte[] result = new byte[4];
        StringTokenizer tokenizer = new StringTokenizer(ip, ".");
        for (int i = 0; i < 4 && tokenizer.hasMoreTokens(); i++) {
            String octet = tokenizer.nextToken();
            int value = Integer.parseInt(octet);
            result[i] = (byte) value;
        }
        return result;
    }
    
    private static byte[] getMask(int size) {
        int bitmask = 0xFFFFFFFF;
        bitmask = bitmask >>> 32 - size;
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) (bitmask % 256);
            bitmask /= 256;
        }
        return result;
    }
}
