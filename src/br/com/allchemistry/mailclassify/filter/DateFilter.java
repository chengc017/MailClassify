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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message;
import br.com.allchemistry.mailclassify.Main;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_NEGATIVE;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_POSITIVE;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class DateFilter extends ClassifyFilter {
    
    private int MAX_HOURS;

    public static final Pattern PATTERN_DATE =
            Pattern.compile("(^|\\s)((Mon|Tue|Wed|Thu|Fri|Sat|Sun),)?"
            + "\\s*?([0-3]?[0-9]) (Jan|Feb|Ma[ry]|Apr|Ju[nl]|Aug|Sep|Oct|Nov|Dec) "
            + "(20[0-9]{2}) ([0-2][0-9](\\:[0-5][0-9]){1,2}) "
            + "([+-][01][0-9][0-5][0-9])(\\s|$)",
            Pattern.CASE_INSENSITIVE);
    
    public DateFilter(String descriptor,
            int max_hours) throws Exception {
        super(descriptor);
        setMaxHours(max_hours);
    }
    
    private DateFilter(ObjectInputStream ois) throws IOException, Exception {
        super(ois);
        MAX_HOURS = ois.readInt();
    }
    
    protected void setMaxHours(int max_hours) throws Exception {
        if (max_hours < 1) {
            throw new Exception("Invalid value.");
        } else {
            MAX_HOURS = max_hours;
        }
    }
    
    @Override
    public void store() throws IOException {
        String fileName = "." + getDescriptor() + ".filter";
        FileOutputStream fos = new FileOutputStream(fileName);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeUTF("DateFilter");
            oos.writeInt(1); // version
            super.store(oos);
            oos.writeInt(MAX_HOURS);
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
    
    public static DateFilter loadOrCreate(String descriptor,
            int max_hours) throws Exception {
        DateFilter filter = load(descriptor);
        if (filter == null) {
            filter = new DateFilter(descriptor, max_hours);
        } else {
            filter.setDescriptor(descriptor);
            filter.setMaxHours(max_hours);
        }
        return filter;
    }
    
    public static DateFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("DateFilter")) {
                        int version = ois.readInt();
                        if (version == 1) {
                            return new DateFilter(ois);
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
    
    public void store(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            super.store(oos);
            oos.close();
        } finally {
            fos.close();
        }
    }
    
    private static int getMonth(String monthName) {
        monthName = monthName.toLowerCase();
        if (monthName.equals("jan")) {
            return GregorianCalendar.JANUARY;
        } else if (monthName.equals("feb")) {
            return GregorianCalendar.FEBRUARY;
        } else if (monthName.equals("mar")) {
            return GregorianCalendar.MARCH;
        } else if (monthName.equals("apr")) {
            return GregorianCalendar.APRIL;
        } else if (monthName.equals("may")) {
            return GregorianCalendar.MAY;
        } else if (monthName.equals("jun")) {
            return GregorianCalendar.JUNE;
        } else if (monthName.equals("jul")) {
            return GregorianCalendar.JULY;
        } else if (monthName.equals("aug")) {
            return GregorianCalendar.AUGUST;
        } else if (monthName.equals("sep")) {
            return GregorianCalendar.SEPTEMBER;
        } else if (monthName.equals("oct")) {
            return GregorianCalendar.OCTOBER;
        } else if (monthName.equals("nov")) {
            return GregorianCalendar.NOVEMBER;
        } else if (monthName.equals("dec")) {
            return GregorianCalendar.DECEMBER;
        } else {
            return -1;
        }
    }
    
    private static final GregorianCalendar CALENDAR = new GregorianCalendar();
    
    private synchronized static Date getDate(Message message, String header) {
        try {
            String[] headers = message.getHeader(header);
            if (headers == null) {
                return null;
            } else {
                for (String text : headers) {
                    Matcher matcher = PATTERN_DATE.matcher(text);
                    while (matcher.find()) {
                        try {
                            int day = Integer.parseInt(matcher.group(4));
                            int month = getMonth(matcher.group(5));
                            int year = Integer.parseInt(matcher.group(6));
                            String time = matcher.group(7);
                            int index1 = time.indexOf(':');
                            int index2 = time.indexOf(':', index1 + 1);
                            int hour = Integer.parseInt(time.substring(0, index1));
                            int minute = Integer.parseInt(time.substring(index1 + 1, index2));
                            int second = Integer.parseInt(time.substring(index2 + 1));
                            CALENDAR.set(GregorianCalendar.YEAR, year);
                            CALENDAR.set(GregorianCalendar.MONTH, month);
                            CALENDAR.set(GregorianCalendar.DAY_OF_MONTH, day);
                            CALENDAR.set(GregorianCalendar.HOUR_OF_DAY, hour);
                            CALENDAR.set(GregorianCalendar.MINUTE, minute);
                            CALENDAR.set(GregorianCalendar.SECOND, second);
                            return CALENDAR.getTime();
                        } catch (Exception exception) {
                        }
                    }
                }
                return null;
            }
        } catch (Exception exception) {
            return null;
        }
    }
    
    private static double hourDiff(Date date1, Date date2) {
        return (double) Math.abs(date1.getTime() - date2.getTime()) / 3600000;
    }
    
    @Override
    public byte trainingNegative(Message message) {
        byte output = classify(message);
        if (output == OUTPUT_NEGATIVE) {
            addSuccessNegative();
        } else if (output == OUTPUT_POSITIVE) {
            addFailurePositive();
        }
        return output;
    }

    @Override
    public byte trainingPositive(Message message) {
        byte output = classify(message);
        if (output == OUTPUT_NEGATIVE) {
            addFailureNegative();
        } else if (output == OUTPUT_POSITIVE) {
            addSuccessPositive();
        }
        return output;
    }

    @Override
    public byte classify(Message message) {
        long startTime = System.currentTimeMillis();
        byte output = OUTPUT_INCONCLUSIVE;
        try {
            String[] header = message.getHeader("Date");
            if (header == null) {
                output = OUTPUT_POSITIVE;
            } else {
                Date receivedDate = getDate(message, "Received");
                Date sentDate = getDate(message, "Date");
                if (receivedDate == null || sentDate == null) {
                    output = OUTPUT_POSITIVE;
                } else if (hourDiff(sentDate, receivedDate) > MAX_HOURS) {
                    output = OUTPUT_POSITIVE;
                }
            }
        } catch (Exception exception) {
            output = OUTPUT_INCONCLUSIVE;
        } finally {
            if (output == OUTPUT_NEGATIVE) {
                addNegativeOutput();
            } else if (output == OUTPUT_POSITIVE) {
                addPositiveOutput();
            } else {
                addInconclusiveOutput();
            }
            long endTime = System.currentTimeMillis();
            addTimeSpent(endTime - startTime);
            return output;
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        DateFilter mailFilter = DateFilter.loadOrCreate("date", 72);
        Main.test(mailFilter, 0.001f, true);
        mailFilter.store();
    }
}
