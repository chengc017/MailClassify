/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;
import br.com.allchemistry.mailclassify.distribution.NormalDistribution;
import br.com.allchemistry.mailclassify.distribution.TrinomialDistribution;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public abstract class SimpleFilter implements Comparable<SimpleFilter> {
    
    private String DESCRIPTOR;
    
    private TrinomialDistribution outputDistribution;
    private BinomialDistribution errorDistribution;
    private NormalDistribution timeSpent;
    
    public SimpleFilter(String descriptor) {
        DESCRIPTOR = descriptor;
        outputDistribution = new TrinomialDistribution(1024);
        errorDistribution = new BinomialDistribution(1024);
        timeSpent = new NormalDistribution(1024);
    }
    
    protected final void setDescriptor(String descriptor) throws Exception {
        if (descriptor == null) {
            throw new Exception("Invalid descriptor.");
        } else {
            DESCRIPTOR = descriptor.trim();
        }
    }
    
    protected void store(ObjectOutputStream oos) throws IOException {
        oos.writeInt(1); // Version
        oos.writeUTF(DESCRIPTOR);
        outputDistribution.store(oos);
        errorDistribution.store(oos);
        timeSpent.store(oos);
    }
    
    protected SimpleFilter(ObjectInputStream ois) throws IOException, Exception {
        int version = ois.readInt();
        if (version == 1) {
            DESCRIPTOR = ois.readUTF();
            outputDistribution = new TrinomialDistribution(ois);
            errorDistribution = new BinomialDistribution(ois);
            timeSpent = new NormalDistribution(ois);
        } else {
            throw new IOException("Version not implemented.");
        }
    }
    
    protected final String getDescriptor() {
        return DESCRIPTOR;
    }
    
    public final float getErrorRatio() {
        return errorDistribution.getSuccessProbability();
    }
    
    public final float getHitRatio() {
        return errorDistribution.getFailureProbability();
    }
    
    protected final void addErrorFailure() {
        errorDistribution.addFailure();
    }
    
    protected final void addErrorSuccess() {
        errorDistribution.addSuccess();
    }
    
    protected final void addTimeSpent(float time) {
        timeSpent.addElement(time);
    }
    
    public final float getTimeSpent() {
        return timeSpent.getAverage();
    }
    
    public float getTimeWasted() {
        return getTimeSpent() * (getInconclusiveRatio() +
                getConclusiveRatio() * getErrorRatio());
    }
    
//    public float getConclusiveTimeWasted() {
//        return getTimeSpent() * getErrorRatio();
//    }
    
    protected final void addPositiveOutput() {
        outputDistribution.addSuccess();
    }
    
    protected final void addInconclusiveOutput() {
        outputDistribution.addInconclusive();
    }
    
    protected final void addNegativeOutput() {
        outputDistribution.addFailure();
    }
    
    public final float getNegativeRatio() {
        return outputDistribution.getFailureProbability();
    }
    
    public final float getInconclusiveRatio() {
        return outputDistribution.getInconclusiveProbability();
    }
    
    public final float getConclusiveRatio() {
        return outputDistribution.getConclusiveProbability();
    }
    
    public final float getPositiveRatio() {
        return outputDistribution.getSuccessProbability();
    }
    
//    @Override
//    public int compareTo(SimpleFilter other) {
//        float thisValue = this.getTimeWasted();
//        float otherValue = other.getTimeWasted();
//        if (thisValue < otherValue) {
//            return -1;
//        } else if (thisValue > otherValue) {
//            return 1;
//        } else {
//            return 0;
//        }
//    }
    
//    @Override
//    public int compareTo(SimpleFilter other) {
//        float thisValue = this.getConclusiveTimeWasted();
//        float otherValue = other.getConclusiveTimeWasted();
//        if (thisValue < otherValue) {
//            return -1;
//        } else if (thisValue > otherValue) {
//            return 1;
//        } else {
//            return 0;
//        }
//    }
    
    @Override
    public int compareTo(SimpleFilter other) {
        float thisValue = this.getHitRatio();
        float otherValue = other.getHitRatio();
        if (thisValue == otherValue) {
            thisValue = this.getConclusiveRatio();
            otherValue = other.getConclusiveRatio();
        }
        if (thisValue > otherValue) {
            return -1;
        } else if (thisValue < otherValue) {
            return 1;
        } else {
            
            return 0;
        }
    }
    
    @Override
    public String toString() {
        return DESCRIPTOR;
    }
}
