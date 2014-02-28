/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message;
import br.com.allchemistry.mailclassify.CIDR;
import br.com.allchemistry.mailclassify.ExpirableMap;
import br.com.allchemistry.mailclassify.Main;
import org.apache.commons.net.whois.WhoisClient;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class CidrFilter extends PatternFilter {

    private static final ArrayList<String> FIELDS = new ArrayList<String>();
    private static final WhoisClient WHOIS = new WhoisClient();
    private static final TreeSet<CIDR> reservadSet = new TreeSet<CIDR>();
    public static final Pattern PATTERN_CIDR =
            Pattern.compile("\\b[0-9]{1,3}(\\.[0-9]{1,3}){1,3}/[0-9]{1,2}\\b",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern PATTERN_INTERVAL =
            Pattern.compile("\\b([0-9]{1,3}(\\.[0-9]{1,3}){3})"
            + " *- *([0-9]{1,3}(\\.[0-9]{1,3}){3})\\b",
            Pattern.CASE_INSENSITIVE);
    
    static {
        try {
            reservadSet.add(new CIDR("0.0.0.0/8"));
            reservadSet.add(new CIDR("10.0.0.0/8"));
            reservadSet.add(new CIDR("100.64.0.0/10"));
            reservadSet.add(new CIDR("127.0.0.0/8"));
            reservadSet.add(new CIDR("169.254.0.0/16"));
            reservadSet.add(new CIDR("172.16.0.0/12"));
            reservadSet.add(new CIDR("192.0.0.0/29"));
            reservadSet.add(new CIDR("192.0.2.0/24"));
            reservadSet.add(new CIDR("192.88.99.0/24"));
            reservadSet.add(new CIDR("192.168.0.0/16"));
            reservadSet.add(new CIDR("198.18.0.0/15"));
            reservadSet.add(new CIDR("198.51.100.0/24"));
            reservadSet.add(new CIDR("203.0.113.0/24"));
            reservadSet.add(new CIDR("224.0.0.0/4"));
            reservadSet.add(new CIDR("240.0.0.0/4"));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    
    private ExpirableMap<CIDR,String> cidrMap;
    
    public CidrFilter(String descriptor) throws Exception {
        super(descriptor, IpFilter.PATTERN_ADDRESS, 2);
        cidrMap = new ExpirableMap<CIDR,String>(105);
    }
    
    public CidrFilter(String descriptor, float precision) throws Exception {
        super(descriptor, IpFilter.PATTERN_ADDRESS, 2, precision);
        cidrMap = new ExpirableMap<CIDR,String>(105);
    }
    
    private CidrFilter(ObjectInputStream ois, int version)
            throws IOException, ClassNotFoundException, Exception {
        super(ois);
        if (version == 1) {
            cidrMap = new ExpirableMap<CIDR,String>(ois);
        } else if (version == 2) {
            if (!ois.readBoolean()) {
                FIELDS.add(ois.readUTF());
            }
            cidrMap = new ExpirableMap<CIDR,String>(ois);
        } else if (version == 3) {
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                FIELDS.add(ois.readUTF());
            }
            cidrMap = new ExpirableMap<CIDR,String>(ois);
        } else {
            throw new IOException("Version not implemented.");
        }
    }
    
    public void addField(String field) throws Exception {
        if (field == null) {
            throw new Exception("Invalid field.");
        } else if (!FIELDS.contains(field)) {
            FIELDS.add(field);
        }
    }
    
    public void setFields(ArrayList<String> fields) throws Exception {
        if (fields == null) {
            throw new Exception("Invalid field list.");
        } else {
            FIELDS.clear();
            for (String field : fields) {
                addField(field);
            }
        }
    }
    
    @Override
    public void store() throws IOException {
        String fileName = "." + getDescriptor() + ".filter";
        FileOutputStream fos = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUTF("CidrFilter");
            oos.writeInt(3); // version
            super.store(oos);
            oos.writeInt(FIELDS.size());
            for (String field : FIELDS) {
                oos.writeUTF(field);
            }
            cidrMap.store(oos);
            oos.flush();
        } finally {
            fos.close();
        }
        File fileNew = new File("." + getDescriptor() + ".filter");
        File fileOld = new File(getDescriptor() + ".filter");
        if (!fileOld.exists() || fileOld.delete()) {
            fileNew.renameTo(fileOld);
        }
    }
    
    public static CidrFilter loadOrCreate(String descriptor) throws Exception {
        CidrFilter filter = load(descriptor);
        if (filter == null) {
            filter = new CidrFilter(descriptor);
        } else {
            filter.setDescriptor(descriptor);
            filter.clearPrecision();
        }
        return filter;
    }
    
    public static CidrFilter loadOrCreate(
            String descriptor, float precision) throws Exception {
        CidrFilter filter = load(descriptor);
        if (filter == null) {
            filter = new CidrFilter(descriptor, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setPrecision(precision);
        }
        return filter;
    }
    
    public static CidrFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("CidrFilter")) {
                        int version = ois.readInt();
                        return new CidrFilter(ois, version);
                    } else {
                        throw new IOException("Incompatible file.");
                    }
                } catch (Exception exception) {
                    throw new IOException("Incompatible file.", exception);
                } finally {
                    fis.close();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }
    
    @Override
    public void downsizeCache(float ratio) {
        try {
            super.downsizeCache(ratio);
            cidrMap.downsize(ratio);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    protected final String[] getTextArray(Message message) {
        try {
            return message.getHeader("Received");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    private String getValue(String text, ArrayList<String> fields) {
        if (text == null) {
            return null;
        } else if (fields.isEmpty()) {
            return null;
        } else {
            StringTokenizer tokenizer = new StringTokenizer(text, "\n");
            while (tokenizer.hasMoreTokens()) {
                String line = tokenizer.nextToken();
                for (String field : fields) {
                    if (line.startsWith(field + ":")) {
                        int index = field.length() - 1;
                        index = line.indexOf(':', index) + 1;
                        return line.substring(index).trim();
                    }
                }
            }
            return null;
        }
    }
    
    private CIDR getCIDR(String text, String ip) throws Exception {
        Matcher matcher = PATTERN_CIDR.matcher(text);
        while (matcher.find()) {
            String element = matcher.group();
            CIDR cidr = new CIDR(element);
            if (cidr.contains(ip)) {
                return cidr;
            }
        }
        matcher = PATTERN_INTERVAL.matcher(text);
        while (matcher.find()) {
            String beginIP = matcher.group(1);
            String endIP = matcher.group(3);
            for (CIDR cidr : CIDR.getCIDRs(beginIP, endIP)) {
                if (cidr.contains(ip)) {
                    return cidr;
                }
            }
        }
        return null;
    }
    
    private CIDR getCIDR(String ip) throws Exception {
        for (CIDR cidr : reservadSet) {
            if (cidr.contains(ip)) {
                return null;
            }
        }
        for (CIDR cidr : cidrMap.keySet()) {
            if (cidr.contains(ip)) {
                return cidr;
            }
        }
        try {
            String server = "whois.arin.net";
            String[] referralServer = {server};
            String text = null;
            while (referralServer != null) {
                server = referralServer[0];
                if (server.startsWith("whois://")) {
                    server = server.substring(8);
                } else if (server.startsWith("rwhois://")) {
                    server = server.substring(9);
                }
                if (server.endsWith("/")) {
                    int index = server.length() - 1;
                    server = server.substring(0, index);
                }
                System.out.println("Consulting WHOIS " + ip + "@" + server + ".");
                int index = server.indexOf(':');
                if (index == -1) {
                    WHOIS.connect(server);
                } else {
                    int port = Integer.parseInt(server.substring(index+1));
                    server = server.substring(0, index);
                    WHOIS.connect(server, port);
                }
                text = WHOIS.query(ip);
                WHOIS.disconnect();
                    
                referralServer = null;
                StringTokenizer tokenizer = new StringTokenizer(text, "\n");
                while (tokenizer.hasMoreTokens()) {
                    String line = tokenizer.nextToken().trim();
                    if (line.startsWith("ReferralServer: ")) {
                        referralServer = new String[1];
                        referralServer[0] = line.substring(16).trim();
                        break;
                    }
                }
            }
            CIDR cidr = getCIDR(text, ip);
            if (cidr == null) {
                System.err.println("CIDR not found for " + ip + ".");
                return null;
            } else {
                String value = getValue(text, FIELDS);
                if (value == null) {
                    System.err.println("Value of field not found for "
                            + ip + " in server " + server + ":\n" + text);
                    return cidr;
                } else {
                    cidrMap.put(cidr, value);
                    return cidr;
                }
            }
        } catch (ConnectException exception) {
            System.err.println("Network error when consulting WHOIS " + ip + ".");
            return null;
        }
    }
    
    @Override
    protected String convert(String element) {
        try {
            CIDR cidr = getCIDR(element);
            if (cidr == null) {
                return null;
            } else {
                String value = cidrMap.get(cidr);
//                if (value == null) {
//                    return cidr.toString();
//                } else {
                    return value;
//                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }
    
    public static void main(String[] args) throws Exception {
        CidrFilter filter = CidrFilter.loadOrCreate("netowner");
        Main.test(filter, 0.001f, true);
        filter.store();
    }
}
