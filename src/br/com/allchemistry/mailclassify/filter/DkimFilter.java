/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeSet;
import javax.mail.Message;
import br.com.allchemistry.mailclassify.Main;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;
import org.apache.james.jdkim.DKIMVerifier;
import org.apache.james.jdkim.exceptions.PermFailException;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class DkimFilter extends ElementFilter {
    
    private static final DKIMVerifier VERIFIER = new DKIMVerifier();
    
    public DkimFilter(String descriptor) throws Exception {
        super(descriptor);
    }
    
    public DkimFilter(String descriptor, float precision) throws Exception {
        super(descriptor, precision);
    }
    
    private DkimFilter(ObjectInputStream ois)
            throws IOException, ClassNotFoundException, Exception {
        super(ois);
    }
    
    @Override
    public void store() throws IOException {
        String fileName = "." + getDescriptor() + ".filter";
        FileOutputStream fos = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUTF("DkimFilter");
            oos.writeInt(1); // version
            super.store(oos);
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
    }
    
    public static DkimFilter loadOrCreate(String descriptor) throws Exception {
        DkimFilter filter = load(descriptor);
        if (filter == null) {
            filter = new DkimFilter(descriptor);
        } else {
            filter.setDescriptor(descriptor);
            filter.clearPrecision();
        }
        return filter;
    }
    
    public static DkimFilter loadOrCreate(String descriptor,
            float precision) throws Exception {
        DkimFilter filter = load(descriptor);
        if (filter == null) {
            filter = new DkimFilter(descriptor, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setPrecision(precision);
        }
        return filter;
    }
    
    public static DkimFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("DkimFilter")) {
                        int version = ois.readInt();
                        if (version == 1) {
                            return new DkimFilter(ois);
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
    
    @Override
    protected byte getOutput(Message message, TreeSet<BinomialDistribution> bdSet) {
        long startTime = System.currentTimeMillis();
        byte output = super.getOutput(message, bdSet);
        try {
            if (output == OUTPUT_NEGATIVE) {
                System.out.println("Consulting DKIM.");
                InputStream inputStream = message.getInputStream();
                VERIFIER.verify(inputStream);
//                System.out.println("DKIM sucess.");
            }
        } catch (PermFailException exception) {
//            System.out.println("DKIM failed.");
            output = OUTPUT_POSITIVE;
        } catch (Exception exception) {
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
            String[] headers = message.getHeader("DKIM-Signature");
            if (headers == null) {
                return null;
            } else {
                TreeSet<BinomialDistribution> set = new TreeSet<BinomialDistribution>();
                for (String header : headers) {
                    int beginIndex = header.indexOf("d=");
                    if (beginIndex != -1) {
                        beginIndex += 2;
                        int endIndex = header.indexOf(';', beginIndex);
                        if (endIndex != -1) {
                            String domain = header.substring(beginIndex, endIndex);
                            domain = domain.toLowerCase();
                            set.add(getDistribution(domain));
                        }
                    }
                }
                if (set.isEmpty()) {
                    return null;
                } else {
                    return set;
                }
            }
        } catch (Exception exception) {
            System.err.println("Error when consulting DKIM.");
            return null;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        DkimFilter mailFilter = DkimFilter.loadOrCreate("dkim");
        Main.test(mailFilter, 0.001f, true);
        mailFilter.store();
    }
}
