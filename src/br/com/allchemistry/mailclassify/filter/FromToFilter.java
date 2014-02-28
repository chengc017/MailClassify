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
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import br.com.allchemistry.mailclassify.Main;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class FromToFilter extends ElementFilter {
    
    public FromToFilter(String descriptor) throws Exception {
        super(descriptor);
    }
    
    public FromToFilter(String descriptor,
            float precision) throws Exception {
        super(descriptor, precision);
    }
    
    private FromToFilter(ObjectInputStream ois)
            throws IOException, Exception {
        super(ois);
    }
    
    @Override
    public void store() throws IOException {
        String fileName = "." + getDescriptor() + ".filter";
        FileOutputStream fos = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUTF("FromToFilter");
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
    
    public static FromToFilter loadOrCreate(String descriptor) throws Exception {
        FromToFilter filter = load(descriptor);
        if (filter == null) {
            filter = new FromToFilter(descriptor);
        } else {
            filter.setDescriptor(descriptor);
            filter.clearPrecision();
        }
        return filter;
    }
    
    public static FromToFilter loadOrCreate(String descriptor,
            float precision) throws Exception {
        FromToFilter filter = load(descriptor);
        if (filter == null) {
            filter = new FromToFilter(descriptor, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setPrecision(precision);
        }
        return filter;
    }
    
    public static FromToFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("FromToFilter")) {
                        int version = ois.readInt();
                        if (version == 1) {
                            return new FromToFilter(ois);
                        } else {
                            throw new IOException("Version not implemented.");
                        }
                    } else {
                        throw new IOException("Incompatible file.");
                    }
                } catch (Exception exception) {
                    throw new IOException("Incompatible file.");
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
    protected final TreeSet<BinomialDistribution> getDistributions(Message message) {
        try {
            TreeSet<String> elementSet = new TreeSet<String>();
            for (Address fromAddress : message.getFrom()) {
                String from = ((InternetAddress)fromAddress).getAddress().toLowerCase();
                for (Address toAddress : message.getRecipients(RecipientType.TO)) {
                    String to = ((InternetAddress)toAddress).getAddress().toLowerCase();
                    elementSet.add(from + ";" + to);
                }
            }
            if (elementSet.isEmpty()) {
                return null;
            } else {
                TreeSet<BinomialDistribution> bdSet = new TreeSet<BinomialDistribution>();
                for (String element : elementSet) {
                    bdSet.add(getDistribution(element));
                }
                return bdSet;
            }
        } catch (Exception exception) {
            return null;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        FromToFilter mailFilter = FromToFilter.loadOrCreate("fromto");
        Main.test(mailFilter, 0.001f, true);
        mailFilter.store();
    }
}
