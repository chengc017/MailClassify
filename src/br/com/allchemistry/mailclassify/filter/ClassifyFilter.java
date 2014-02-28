/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import javax.mail.Message;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public abstract class ClassifyFilter extends SimpleFilter {

    private BinomialDistribution negativeEficience;
    private BinomialDistribution positiveEficience;
    
    public static final byte OUTPUT_NEGATIVE = -1;
    public static final byte OUTPUT_INCONCLUSIVE = 0;
    public static final byte OUTPUT_POSITIVE = 1;

    
    public ClassifyFilter(String descriptor) {
        super(descriptor);
        negativeEficience = new BinomialDistribution(1024);
        positiveEficience = new BinomialDistribution(1024);
    }
    
    protected ClassifyFilter(ObjectInputStream ois) throws IOException, Exception {
        super(ois);
        int version = ois.readInt();
        if (version == 1) {
            negativeEficience = new BinomialDistribution(ois);
            positiveEficience = new BinomialDistribution(ois);
        } else {
            throw new IOException("Version not implemented.");
        }
    }
    
    @Override
    protected void store(ObjectOutputStream oos) throws IOException {
        super.store(oos);
        oos.writeInt(1); // Version
        negativeEficience.store(oos);
        positiveEficience.store(oos);
    }
    
    public final float getNegativeEficience() {
        return negativeEficience.getSuccessProbability();
    }
    
    public final float getPositiveEficience() {
        return positiveEficience.getSuccessProbability();
    }
    
    protected final void addSuccessPositive() {
        positiveEficience.addSuccess();
        addErrorFailure();
    }
    
    protected final void addFailurePositive() {
        positiveEficience.addFailure();
        addErrorSuccess();
    }
    
    protected final void addSuccessNegative() {
        negativeEficience.addSuccess();
        addErrorFailure();
    }
    
    protected final void addFailureNegative() {
        negativeEficience.addFailure();
        addErrorSuccess();
    }

    public abstract byte classify(Message message);
    
    public void printStatistics(PrintStream printStream) {
        printStream.println("Filter: " + toString());
        printStream.println("Time spent: " + getTimeSpent());
        if (getConclusiveRatio() > 0) {
            printStream.println("Conclusive ratio: " + getConclusiveRatio());
            printStream.println("Hit ratio: " + getHitRatio());
            if (getNegativeRatio() > 0) {
                printStream.println("Negative ratio: " + getNegativeRatio());
                printStream.println("Negative eficience: " + getNegativeEficience());
            }
            if (getPositiveRatio() > 0) {
                printStream.println("Positive ratio: " + getPositiveRatio());
                printStream.println("Positive eficience: " + getPositiveEficience());
            }
        }
    }
    
    public abstract byte trainingNegative(Message message);
    
    public abstract byte trainingPositive(Message message);
    
    public abstract void store() throws IOException;
    
    public abstract void downsizeCache(float ratio);
}
