/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import br.com.allchemistry.mailclassify.Main;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.regex.Pattern;
import javax.mail.Message;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class AddressFilter extends PatternFilter {
    
    private String HEADER;
    
    public static final Pattern PATTERN_ADDRESS =
            Pattern.compile("(^|[^a-z0-9@._-])([a-z0-9][a-z0-9._-]*"
            + "@([a-z0-9]+[._-])*[a-z0-9]+)([^a-z0-9@._-]|$)",
            Pattern.CASE_INSENSITIVE);
    
    public AddressFilter(String descritor, String header) throws Exception {
        super(descritor, PATTERN_ADDRESS, 2);
        setHeader(header);
    }
    
    public AddressFilter(String descritor, String header,
            float precision) throws Exception {
        super(descritor, PATTERN_ADDRESS, 2, precision);
        setHeader(header);
    }
    
    private AddressFilter(ObjectInputStream ois)
            throws IOException, ClassNotFoundException, Exception {
        super(ois);
        HEADER = ois.readUTF();
    }
    
    protected void setHeader(String header) throws Exception {
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
            oos.writeUTF("AddressFilter");
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

    public static AddressFilter loadOrCreate(String descriptor,
            String header) throws Exception {
        AddressFilter filter = load(descriptor);
        if (filter == null) {
            filter = new AddressFilter(descriptor, header);
        } else {
            filter.setDescriptor(descriptor);
            filter.setHeader(header);
            filter.clearPrecision();
        }
        return filter;
    }
    
    public static AddressFilter loadOrCreate(String descriptor,
            String header, float precision) throws Exception {
        AddressFilter filter = load(descriptor);
        if (filter == null) {
            filter = new AddressFilter(descriptor, header, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setPrecision(precision);
            filter.setHeader(header);
        }
        return filter;
    }
    
    public static AddressFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("AddressFilter")) {
                        int version = ois.readInt();
                        if (version == 1) {
                            return new AddressFilter(ois);
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
        return element.toLowerCase();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        AddressFilter mailFilter = AddressFilter.loadOrCreate("fromaddress", "From");
        Main.test(mailFilter, 0.001f, true);
        mailFilter.store();
    }
}
