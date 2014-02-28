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
import java.util.regex.Pattern;
import javax.mail.Message;
import br.com.allchemistry.mailclassify.Main;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class HostFilter extends PatternFilter {
    
    private String HEADER;
    
    public static final Pattern PATTERN_HOST =
            Pattern.compile("@(([a-z0-9]+[._-])*[a-z0-9]+)"
            + "([^a-z0-9@._-]|$)", Pattern.CASE_INSENSITIVE);
    
    public HostFilter(String descriptor, String header) throws Exception {
        super(descriptor, PATTERN_HOST, 1);
        setHeader(header);
    }
    
    public HostFilter(String descriptor, String header,
            float precision) throws Exception {
        super(descriptor, PATTERN_HOST, 1, precision);
        setHeader(header);
    }
    
    private HostFilter(ObjectInputStream ois)
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
            oos.writeUTF("HostFilter");
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
    
    public static HostFilter loadOrCreate(String descriptor,
            String header) throws Exception {
        HostFilter filter = load(descriptor);
        if (filter == null) {
            filter = new HostFilter(descriptor, header);
        } else {
            filter.setDescriptor(descriptor);
            filter.setHeader(header);
            filter.clearPrecision();
        }
        return filter;
    }
    
    public static HostFilter loadOrCreate(String descriptor,
            String header, float precision) throws Exception {
        HostFilter filter = load(descriptor);
        if (filter == null) {
            filter = new HostFilter(descriptor, header, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setPrecision(precision);
            filter.setHeader(header);
        }
        return filter;
    }
    
    public static HostFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("HostFilter")) {
                        int version = ois.readInt();
                        if (version == 1) {
                            return new HostFilter(ois);
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
        HostFilter mailFilter = HostFilter.loadOrCreate("senderhost", "Received");
        Main.test(mailFilter, 0.001f, true);
        mailFilter.store();
    }
}
