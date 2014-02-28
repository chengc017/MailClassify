/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public abstract class PatternFilter extends ElementFilter {
    
    private final Pattern PATTERN;
    private final int GROUP;
    
    public PatternFilter(String descriptor,
            Pattern pattern, int group) throws Exception {
        super(descriptor);
        PATTERN = pattern;
        GROUP = group;
    }
    
    public PatternFilter(String descriptor, Pattern pattern,
            int group, float precision) throws Exception {
        super(descriptor, precision);
        PATTERN = pattern;
        GROUP = group;
    }
    
    protected PatternFilter(ObjectInputStream ois)
            throws IOException, ClassNotFoundException, Exception {
        super(ois);
        int version = ois.readInt();
        if (version == 1) {
            PATTERN = (Pattern) ois.readObject();
            GROUP = ois.readInt();
        } else {
            throw new IOException("Version not implemented.");
        }
    }
    
    @Override
    protected void store(ObjectOutputStream oos) throws IOException {
        super.store(oos);
        oos.writeInt(1); // Version
        oos.writeObject(PATTERN);
        oos.writeInt(GROUP);        
    }
    
    protected abstract String[] getTextArray(Message message);
    
    protected abstract String convert(String element);
    
    @Override
    protected final TreeSet<BinomialDistribution> getDistributions(Message message) {
        try {
            String[] header = getTextArray(message);
            if (header == null) {
                return null;
            } else {
                TreeSet<String> elementSet = new TreeSet<String>();
                for (String text : header) {
                    Matcher matcher = PATTERN.matcher(text);
                    while (matcher.find()) {
                        String element = matcher.group(GROUP);
                        element = convert(element);
                        if (element != null) {
                            elementSet.add(element);
                        }
                    }
                }
                if (elementSet.isEmpty()) {
                    return null;
                } else {
                    TreeSet<BinomialDistribution> set = new TreeSet<BinomialDistribution>();
                    for (String element : elementSet) {
                        set.add(getDistribution(element));
                    }
//                    System.out.println(elementSet);
                    return set;
                }
            }
        } catch (Exception exception) {
            return null;
        }
    }
}
