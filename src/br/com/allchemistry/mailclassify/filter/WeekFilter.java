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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TreeSet;
import javax.mail.Message;
import br.com.allchemistry.mailclassify.Main;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class WeekFilter extends IndexFilter {
    
    private final GregorianCalendar CALENDAR = new GregorianCalendar();
    
    public WeekFilter(String descriptor) throws Exception {
        super(descriptor, 7);
    }
    
    public WeekFilter(String descriptor,
            float precision) throws Exception {
        super(descriptor, precision, 7);
    }
    
    private WeekFilter(ObjectInputStream ois) throws IOException, Exception {
        super(ois);
    }
    
    @Override
    public void store() throws IOException {
        String fileName = "." + getDescriptor() + ".filter";
        FileOutputStream fos = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUTF("WeekFilter");
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
    
    public static WeekFilter loadOrCreate(String descriptor) throws Exception {
        WeekFilter filter = load(descriptor);
        if (filter == null) {
            filter = new WeekFilter(descriptor);
        } else {
            filter.setDescriptor(descriptor);
            filter.clearPrecision();
        }
        return filter;
    }
    
    public static WeekFilter loadOrCreate(String descriptor,
            float precision) throws Exception {
        WeekFilter filter = load(descriptor);
        if (filter == null) {
            filter = new WeekFilter(descriptor, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setPrecision(precision);
        }
        return filter;
    }
    
    public static WeekFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("WeekFilter")) {
                        int version = ois.readInt();
                        if (version == 1) {
                            return new WeekFilter(ois);
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
    protected final TreeSet<BinomialDistribution> getDistributions(Message message) {
        try {
            Date receivedDate = message.getSentDate();
            if (receivedDate == null) {
                return null;
            } else {
                CALENDAR.setTime(receivedDate);
                int day = CALENDAR.get(GregorianCalendar.DAY_OF_WEEK-1);
                TreeSet<BinomialDistribution> set = new TreeSet<BinomialDistribution>();
                set.add(getDistribution(day));
                return set;
            }
        } catch (Exception exception) {
            return null;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        WeekFilter mailFilter = WeekFilter.loadOrCreate("weekday");
        Main.test(mailFilter, 0.001f, true);
        mailFilter.store();
    }
}
