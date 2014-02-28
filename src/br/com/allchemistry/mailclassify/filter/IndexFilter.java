/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public abstract class IndexFilter extends AjustableFilter {
    
    private BinomialDistribution[] bdArray;
    
    public IndexFilter(String descriptor,
            int size) throws Exception {
        super(descriptor);
        bdArray = new BinomialDistribution[size];
        for (int i = 0; i < size; i++) {
            bdArray[i] = new BinomialDistribution();
        }
    }
    
    public IndexFilter(String descriptor,
            float precision, int size) throws Exception {
        super(descriptor, precision);
        bdArray = new BinomialDistribution[size];
        for (int i = 0; i < size; i++) {
            bdArray[i] = new BinomialDistribution();
        }
    }
    
    public IndexFilter(ObjectInputStream ois) throws IOException, Exception {
        super(ois);
        int version = ois.readInt();
        if (version == 1) {
            int size = ois.readInt();
            bdArray = new BinomialDistribution[size];
            for (int i = 0; i < size; i++) {
                bdArray[i] = new BinomialDistribution(ois);
            }
        } else {
            throw new IOException("Version not implemented.");
        }
    }
    
    @Override
    public void store(ObjectOutputStream oos) throws IOException {
        super.store(oos);
        oos.writeInt(1); // Version
        int size = bdArray.length;
        oos.writeInt(size);
        for (int i = 0; i < size; i++) {
            bdArray[i].store(oos);
        }
    }
    
    protected final BinomialDistribution getDistribution(int index) {
        return bdArray[index];
    }
}
