/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class CIDR implements Comparable<CIDR> {
    
    private int number;
    private byte mask;
    
    private CIDR(int ip, byte mask) throws Exception {
        setIP(ip);
        setMask(mask);
    }
    
    public CIDR(String cidr) throws Exception {
        int index = cidr.indexOf('/');
        if (index != -1) {
            setIP(cidr.substring(0, index));
            setMask(Integer.parseInt(cidr.substring(++index)));
        } else {
            throw new Exception("Not implemented.");
        }
    }
    
    public CIDR(ObjectInputStream ois) throws IOException, Exception {
        int version = ois.readInt();
        if (version == 1) {
            number = ois.readInt();
            mask = ois.readByte();
        } else {
            throw new Exception("Invalid version.");
        }
    }
    
    public void store(ObjectOutputStream oos) throws IOException {
        oos.writeInt(1); // Version.
        oos.writeInt(number);
        oos.writeByte(mask);
    }
    
    public void setIP(String ip) throws Exception {
        number = convertIP(ip);
    }
    
    public void setIP(int ip) throws Exception {
        number = ip;
    }
    
    public void setMask(int size) throws Exception {
        if (size < 0 || size > 31) {
            throw new Exception("Invalid mask size.");
        } else {
            mask = (byte) (32 - size);
        }
    }
    
    private static int convertIP(String ip) throws Exception {
        int number = 0;
        StringTokenizer tokenizer = new StringTokenizer(ip, ".");
        if (tokenizer.countTokens() > 4) {
            throw new Exception("Invalid IP address.");
        } else {
            for (int i = 0; i < 4; i++) {
                number = number << 8;
                if (tokenizer.hasMoreTokens()) {
                    try {
                        String octet = tokenizer.nextToken();
                        int value = Integer.parseInt(octet);
                        number = number ^ value;
                    } catch (NumberFormatException exception) {
                        throw new Exception("Invalid IP address.");
                    }
                }
            }
            return number;
        }
    }
    
    public boolean contains(String ip) throws Exception {
        int other = convertIP(ip);
        return contains(other);
    }
    
    private boolean contains(int other) throws Exception {
        return (number >>> mask) == (other >>> mask);
    }
    
    public int getNet() {
        return number >>> mask << mask - 1;
    }
    
    @Override
    public String toString() {
        int temp = number >>> mask << mask;
        String cidr = null;
        while (temp != 0) {
            if (cidr == null) {
                cidr = Integer.toString(temp & 255);
            } else {
                cidr = Integer.toString(temp & 255) + "." + cidr;
            }
            temp = temp >>> 8;
        }
        cidr += "/" + (32 - mask);
        return cidr;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof CIDR) {
            CIDR other = (CIDR) o;
            return this.number == other.number
                    && this.mask == other.mask;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return number >>> mask;
    }
    
    @Override
    public int compareTo(CIDR other) {
        int thisNet = this.getNet();
        int otherNet = other.getNet();
        if (thisNet < otherNet) {
            return -1;
        } else if (thisNet > otherNet) {
            return 1;
        } else {
            return 0;
        }
    }
    
        
    public static TreeSet<CIDR> getCIDRs(String begintIP,
            String endIP) throws Exception {
        int begintIntIP = convertIP(begintIP);
        int endIntIP = convertIP(endIP);
        if (begintIntIP > endIntIP) {
            throw new Exception("Invalid interval: " + begintIntIP + "-" + endIntIP + ".");
        }
        TreeSet<CIDR> cidrSet = new TreeSet<CIDR>();
        byte count = 32;
        int mask = 0xFFFFFFFF;
        while ((begintIntIP & mask) != (endIntIP & mask)) {
            mask = mask << 1;
            count--;
        }
        CIDR cidr = new CIDR(begintIntIP, count);
        cidrSet.add(cidr);
        if (!cidr.contains(begintIntIP)) {
            throw new Exception("Not implemented yet.");
        }
        if (!cidr.contains(endIntIP)) {
            throw new Exception("Not implemented yet.");
        }
        return cidrSet;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(CIDR.getCIDRs("209.167.231.0", "209.167.231.255")); // 209.167.0.0/16
//        System.out.println(new CIDR("200.159.21.72/29"));
//        TreeSet<CIDR> cidrSet = new TreeSet<CIDR>();
//        cidrSet.add(new CIDR("192.168.1.0/24"));
//        cidrSet.add(new CIDR("192.168.3.0/24"));
//        System.out.println(cidrSet);
//        CIDR cidr = new CIDR("192.168.1.0/24");
//        System.out.println(cidr.contains("192.168.0.1"));
//        System.out.println(cidr.contains("192.168.1.1"));
//        System.out.println(cidr.contains("192.168.2.1"));
    }

}
