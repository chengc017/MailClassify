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
import javax.mail.Message;
import br.com.allchemistry.mailclassify.Main;
import br.com.allchemistry.mailclassify.utilities.TDL;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class DomainFilter extends PatternFilter {
    
    private String HEADER;
    
    public DomainFilter(String descriptor, String header) throws Exception {
        super(descriptor, HostFilter.PATTERN_HOST, 1);
        setHeader(header);
    }
    
    public DomainFilter(String descriptor, String header,
            float precision) throws Exception {
        super(descriptor, HostFilter.PATTERN_HOST, 1, precision);
        setHeader(header);
    }
    
    private DomainFilter(ObjectInputStream ois)
            throws IOException, ClassNotFoundException, Exception {
        super(ois);
        HEADER = ois.readUTF();
    }
    
    private void setHeader(String header) throws Exception {
        if (header == null) {
            throw new Exception("Invalid header.");
        } else {
            HEADER = header;
        }
    }
    
    @Override
    public void store() throws IOException {
        String fileName = "." + getDescriptor() + ".filter";
        FileOutputStream fos = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUTF("DomainFilter");
            oos.writeInt(1); // version
            super.store(oos);
            oos.writeUTF(HEADER);
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
    
    public static DomainFilter loadOrCreate(String descriptor,
            String header) throws Exception {
        DomainFilter filter = load(descriptor);
        if (filter == null) {
            filter = new DomainFilter(descriptor, header);
        } else {
            filter.setDescriptor(descriptor);
            filter.setHeader(header);
            filter.clearPrecision();
        }
        return filter;
    }
    
    public static DomainFilter loadOrCreate(String descriptor,
            String header, float precision) throws Exception {
        DomainFilter filter = load(descriptor);
        if (filter == null) {
            filter = new DomainFilter(descriptor, header, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setPrecision(precision);
            filter.setHeader(header);
        }
        return filter;
    }
    
    public static DomainFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("DomainFilter")) {
                        int version = ois.readInt();
                        if (version == 1) {
                            return new DomainFilter(ois);
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
    protected final String[] getTextArray(Message message) {
        try {
            return message.getHeader(HEADER);
        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    protected final String convert(String element) {
        return TDL.extractDomain(element.toLowerCase());
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        DomainFilter mailFilter = DomainFilter.loadOrCreate("fromdomain", "From");
        Main.test(mailFilter, 0.001f, true);
        mailFilter.store();
    }
}
