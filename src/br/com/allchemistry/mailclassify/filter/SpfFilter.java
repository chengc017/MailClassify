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
import java.util.TreeSet;
import java.util.regex.Matcher;
import javax.mail.Message;
import javax.mail.MessagingException;
import br.com.allchemistry.mailclassify.ExpirableMap;
import br.com.allchemistry.mailclassify.Main;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_INCONCLUSIVE;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_NEGATIVE;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_POSITIVE;
import org.apache.james.jspf.impl.DefaultSPF;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class SpfFilter extends ElementFilter {
    
    private static final DefaultSPF SPF = new DefaultSPF();
    private ExpirableMap<String,String> resultMap;
    
    public SpfFilter(String descriptor) throws Exception {
        super(descriptor);
        resultMap = new ExpirableMap<String,String>(7);
        SPF.setTimeOut(3);
    }
    
    public SpfFilter(String descriptor,
            float precision) throws Exception {
        super(descriptor, precision);
        resultMap = new ExpirableMap<String,String>(7);
        SPF.setTimeOut(3);
    }
    
    private SpfFilter(ObjectInputStream ois)
            throws IOException, ClassNotFoundException, Exception {
        super(ois);
        resultMap = new ExpirableMap(ois);
    }
    
    @Override
    public void store() throws IOException {
        String fileName = "." + getDescriptor() + ".filter";
        FileOutputStream fos = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUTF("SpfFilter");
            oos.writeInt(1); // version
            super.store(oos);
            resultMap.store(oos);
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
    
    @Override
    public void downsizeCache(float ratio) {
        try {
            super.downsizeCache(ratio);
            resultMap.downsize(ratio);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static SpfFilter loadOrCreate(String descriptor) throws Exception {
        SpfFilter filter = load(descriptor);
        if (filter == null) {
            filter = new SpfFilter(descriptor);
        } else {
            filter.setDescriptor(descriptor);
            filter.clearPrecision();
        }
        return filter;
    }
    
    public static SpfFilter loadOrCreate(String descriptor,
            float precision) throws Exception {
        SpfFilter filter = load(descriptor);
        if (filter == null) {
            filter = new SpfFilter(descriptor, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setPrecision(precision);
        }
        return filter;
    }
    
    public static SpfFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("SpfFilter")) {
                        int version = ois.readInt();
                        if (version == 1) {
                            return new SpfFilter(ois);
                        } else {
                            throw new IOException("Version not implemented.");
                        }
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
    
    private static String getHostIP(
            Message message) throws MessagingException {
        String[] headers = message.getHeader("Received");
        if (headers == null) {
            return null;
        } else {
            for (String header : headers) {
                if (header.startsWith("from ") && header.contains("by ")) {
                    int beginIndex = 5;
                    int endIndex = header.indexOf("by ", beginIndex);
                    String from = header.substring(beginIndex, endIndex).trim();
                    Matcher matcher = IpFilter.PATTERN_ADDRESS.matcher(from);
                    while (matcher.find()) {
                        String ip = matcher.group(2);
                        return ip;
                    }
                } else {
                    return null;
                }
            }
            return null;
        }
    }
    
    private static TreeSet<String> getAddresses(Message message,
            String header) throws MessagingException {
        TreeSet<String> addressList = new TreeSet<String>();
        String[] headers = message.getHeader(header);
        if (headers != null) {
            for (String fromHeader : headers) {
                Matcher matcher = AddressFilter.PATTERN_ADDRESS.matcher(fromHeader);
                while (matcher.find()) {
                    String address = matcher.group(2).toLowerCase();
                    addressList.add(address);
                }
            }
        }
        return addressList;
    }
    
    @Override
    protected byte getOutput(Message message, TreeSet<BinomialDistribution> bdSet) {
        long startTime = System.currentTimeMillis();
        byte output = super.getOutput(message, bdSet);
        try {
            if (output == OUTPUT_NEGATIVE) {
                String ip = getHostIP(message);
                if (ip == null) {
                    output = OUTPUT_INCONCLUSIVE;
                } else {
                    for (String address : getAddresses(message, "Return-path")) {
                        String result;
                        if (resultMap.containsKey(address)) {
                            result = resultMap.get(address);
                        } else {
                            try {
                                System.out.println("Consulting SPF " + ip + ";" + address + ".");
                                result = SPF.checkSPF(ip, address, "").getResult();
                                resultMap.put(address, result);
                                if (result.equals("pass")) {
                                    output = OUTPUT_NEGATIVE;
                                    break;
                                } else if (result.equals("fail")) {
                                    output = OUTPUT_POSITIVE;
                                    break;
                                }
                            } catch (Exception exception) {
                                System.err.println(
                                        "Error when consulting SPF "
                                        + ip + ";" + address  + ": "
                                        + exception.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            output = OUTPUT_INCONCLUSIVE;
        } finally {
            long endTime = System.currentTimeMillis();
            addTimeSpent(endTime - startTime);
            return output;
        }
    }
    
    @Override
    protected final TreeSet<BinomialDistribution> getDistributions(Message message) {
        try {
            String[] headers = message.getHeader("Return-path");
            if (headers == null) {
                return null;
            } else {
                TreeSet<BinomialDistribution> set = new TreeSet<BinomialDistribution>();
                for (String address : getAddresses(message, "Return-path")) {
                    set.add(getDistribution(address));
                }
                if (set.isEmpty()) {
                    return null;
                } else {
                    return set;
                }
            }
        } catch (Exception exception) {
            return null;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        SpfFilter mailFilter = SpfFilter.loadOrCreate("spf");
        Main.test(mailFilter, 0.001f, true);
        mailFilter.store();
    }
}
