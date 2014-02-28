/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import br.com.allchemistry.mailclassify.ExpirableMap;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public abstract class ElementFilter extends AjustableFilter {
    
    private final int POPULATION;
    private ExpirableMap<String,BinomialDistribution> elementMap;
    
    public ElementFilter(String descriptor) throws Exception {
        super(descriptor);
        POPULATION = 128;
        elementMap = new ExpirableMap<String,BinomialDistribution>();
    }
    
    public ElementFilter(String descriptor,
            float precision
            ) throws Exception {
        super(descriptor, precision);
        POPULATION = 128;
        elementMap = new ExpirableMap<String,BinomialDistribution>();
    }
    
    protected ElementFilter(ObjectInputStream ois)
            throws IOException, Exception {
        super(ois);
        int version = ois.readInt();
        if (version == 1) {
            POPULATION = ois.readInt();
            elementMap = new ExpirableMap<String,BinomialDistribution>(ois);
        } else {
            throw new IOException("Version not implemented.");
        }
    }
    
    @Override
    protected void store(ObjectOutputStream oos) throws IOException {
        super.store(oos);
        oos.writeInt(1); // Version
        oos.writeInt(POPULATION);
        elementMap.store(oos);
    }
    
    @Override
    public void downsizeCache(float ratio) {
        try {
            elementMap.downsize(ratio);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    protected final synchronized BinomialDistribution getDistribution(String element) {
        try {
            if (elementMap.containsKey(element)) {
                return elementMap.get(element);
            } else {
                BinomialDistribution bd = new BinomialDistribution(POPULATION);
                elementMap.put(element, bd);
                return bd;
            }
        } catch (Exception exception) {
            return null;
        }
    }
}
