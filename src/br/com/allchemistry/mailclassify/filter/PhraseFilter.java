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
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message;
import br.com.allchemistry.mailclassify.Main;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;
import br.com.allchemistry.mailclassify.utilities.TextStriper;
import br.com.allchemistry.mailclassify.utilities.Utilities;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public final class PhraseFilter extends ElementFilter {
    
    private String HEADER;
    
    public static final Pattern PATTERN_WORD =
            Pattern.compile("(^|[^a-zA-ZÀ-ú])([a-zA-ZÀ-ú]{4,16})([^a-zA-ZÀ-ú]|$)",
            Pattern.CASE_INSENSITIVE);
    
    public PhraseFilter(String descriptor) throws Exception {
        super(descriptor);
        HEADER = null;
    }
    
    public PhraseFilter(String descriptor,
            float precision) throws Exception {
        super(descriptor, precision);
        HEADER = null;
    }
    
    public PhraseFilter(String descriptor, String header) throws Exception {
        super(descriptor);
        setHeader(header);
    }
    
    public PhraseFilter(String descriptor, String header,
            float precision) throws Exception {
        super(descriptor, precision);
        setHeader(header);
    }
    
    private PhraseFilter(ObjectInputStream ois)
            throws IOException, ClassNotFoundException, Exception {
        super(ois);
        boolean headerIsNull = ois.readBoolean();
        if (headerIsNull) {
            HEADER = null;
        } else {
            HEADER = ois.readUTF();
        }
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
            oos.writeUTF("PhraseFilter");
            oos.writeInt(1); // version
            super.store(oos);
            oos.writeBoolean(HEADER == null);
            if (HEADER != null) {
                oos.writeUTF(HEADER);
            }
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
    
    public static PhraseFilter loadOrCreate(String descriptor) throws Exception {
        PhraseFilter filter = load(descriptor);
        if (filter == null) {
            filter = new PhraseFilter(descriptor);
        } else {
            filter.setDescriptor(descriptor);
            filter.clearPrecision();
        }
        return filter;
    }
    
    public static PhraseFilter loadOrCreate(String descriptor,
            float precision) throws Exception {
        PhraseFilter filter = load(descriptor);
        if (filter == null) {
            filter = new PhraseFilter(descriptor, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setPrecision(precision);
        }
        return filter;
    }
    
    public static PhraseFilter loadOrCreate(String descriptor,
            String header) throws Exception {
        PhraseFilter filter = load(descriptor);
        if (filter == null) {
            filter = new PhraseFilter(descriptor, header);
        } else {
            filter.setDescriptor(descriptor);
            filter.setHeader(header);
            filter.clearPrecision();
        }
        return filter;
    }
    
    public static PhraseFilter loadOrCreate(String descriptor,
            String header, float precision) throws Exception {
        PhraseFilter filter = load(descriptor);
        if (filter == null) {
            filter = new PhraseFilter(descriptor, header, precision);
        } else {
            filter.setDescriptor(descriptor);
            filter.setHeader(header);
            filter.setPrecision(precision);
        }
        return filter;
    }
    
    public static PhraseFilter load(String descriptor) {
        String fileName = descriptor + ".filter";
        File file = new File(fileName);
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    String className = ois.readUTF();
                    if (className.equals("PhraseFilter")) {
                        int version = ois.readInt();
                        if (version == 1) {
                            return new PhraseFilter(ois);
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
            String[] textArray;
            if (HEADER == null) {
                String text = TextStriper.getText(message);
                text = Utilities.simplify(text);
                StringTokenizer tokenizer = new StringTokenizer(text, "\n");
                textArray = new String[tokenizer.countTokens()];
                int i = 0;
                while (tokenizer.hasMoreTokens()) {
                    textArray[i++] = tokenizer.nextToken();
                }
            } else if (HEADER.equals("Subject")) {
                textArray = new String[1];
                textArray[0] = message.getSubject();
            } else {
                textArray = message.getHeader(HEADER);
            }
            if (textArray == null) {
                return null;
            } else {
                TreeSet<String> elementSet = new TreeSet<String>();
                for (String text : textArray) {
                    LinkedList<String> wordList = new LinkedList<String>();
                    Matcher matcher = PATTERN_WORD.matcher(text);
                    while (matcher.find()) {
                        String word = matcher.group(2).toLowerCase();
                        elementSet.add(word);
                        BinomialDistribution bd = getDistribution(word);
                        if (bd.getPopulation() >= 30) {
                            wordList.add(word);
                        }
                    }
                    while (wordList.size() > 1) {
                        int count = 0;
                        String word1 = wordList.pollFirst();
                        for (String word2 : wordList) {
                            int compare = word1.compareTo(word2);
                            if (compare < 0) {
                                elementSet.add(word1 + ";" + word2);
                            } else if (compare > 0) {
                                elementSet.add(word2 + ";" + word1);
                            }
                            if (compare != 0 && ++count == 5) {
                                break;
                            }
                        }
                    }
                }
                if (elementSet.isEmpty()) {
                    return null;
                } else {
                    TreeSet<BinomialDistribution> bdSet = new TreeSet<BinomialDistribution>();
                    for (String element : elementSet) {
                        bdSet.add(getDistribution(element));
                    }
//                    System.out.println(elementSet);
                    return bdSet;
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
        PhraseFilter mailFilter = PhraseFilter.loadOrCreate("contentsubject", "Subject");
        Main.test(mailFilter, 0.001f, true);
        mailFilter.store();
    }
}
