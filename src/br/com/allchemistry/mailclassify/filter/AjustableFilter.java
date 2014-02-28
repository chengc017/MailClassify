/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.filter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeSet;
import javax.mail.Message;
import br.com.allchemistry.mailclassify.distribution.BinomialDistribution;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_INCONCLUSIVE;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_NEGATIVE;
import static br.com.allchemistry.mailclassify.filter.ClassifyFilter.OUTPUT_POSITIVE;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public abstract class AjustableFilter extends ClassifyFilter {
    
    private float PRECISION;
    private float negativeThreshold;
    private float positiveThreshold;
    
    public AjustableFilter(String descriptor) throws Exception {
        super(descriptor);
        clearPrecision();
        negativeThreshold = 0.5f;
        positiveThreshold = 0.5f;
    }
    
    public AjustableFilter(String descriptor, float precision) throws Exception {
        super(descriptor);
        setPrecision(precision);
        negativeThreshold = 0.5f;
        positiveThreshold = 0.5f;
    }
    
    protected AjustableFilter(ObjectInputStream ois) throws IOException, Exception {
        super(ois);
        int version = ois.readInt();
        if (version == 1) {
            PRECISION = ois.readFloat();
            negativeThreshold = ois.readFloat();
            positiveThreshold = ois.readFloat();
        } else {
            throw new IOException("Version not implemented.");
        }
    }
    
    protected final void setPrecision(float precision) throws Exception {
        if (precision <= 0.0f || precision >= 1.0f) {
            throw new Exception("Invalid value.");
        } else {
            PRECISION = precision;
        }
    }
    
    protected final void clearPrecision() throws Exception {
        PRECISION = 0.0f;
    }
    
    @Override
    protected void store(ObjectOutputStream oos) throws IOException {
        super.store(oos);
        oos.writeInt(1); // Version
        oos.writeFloat(PRECISION);
        oos.writeFloat(negativeThreshold);
        oos.writeFloat(positiveThreshold);
    }
    
    private float getPrecision() {
        if (PRECISION == 0.0f) {
            float precision = 2 * getConclusiveRatio();
            return precision > 1.0f ? 1.0f : precision;
        } else {
            return PRECISION;
        }
    }
    
    protected abstract TreeSet<BinomialDistribution> getDistributions(Message message);
    
    private void ajustNegativeThreshold() {
        float precision = getPrecision();
        float eficience = getNegativeEficience();
        if (eficience < precision) {
            negativeThreshold = (1023 * negativeThreshold + precision) / 1024;
        } else if (eficience > precision) {
            negativeThreshold = (1023 * negativeThreshold + 0.0f) / 1024;
        }
    }
    
    private void ajustPositiveThreshold() {
        float precision = getPrecision();
        float eficience = getPositiveEficience();
        if (eficience < precision) {
            positiveThreshold = (1023 * positiveThreshold + precision) / 1024;
        } else if (eficience > precision) {
            positiveThreshold = (1023 * positiveThreshold + 0.0f) / 1024;
        }
    }
    
    protected byte getOutput(Message message, TreeSet<BinomialDistribution> bdSet) {
        if (message == null) {
            return OUTPUT_INCONCLUSIVE;
        } else if (bdSet == null) {
            bdSet = getDistributions(message);
        }
        if (bdSet == null) {
            return OUTPUT_INCONCLUSIVE;
        } else {
            BinomialDistribution first = bdSet.first();
            BinomialDistribution last = bdSet.last();
            float failureValue = first.getFailureProbability();
            float sucessValue = last.getSuccessProbability();
            if (failureValue > sucessValue && failureValue > negativeThreshold) {
                return OUTPUT_NEGATIVE;
            } else if (sucessValue > failureValue && sucessValue > positiveThreshold) {
                return OUTPUT_POSITIVE;
            } else {
                return OUTPUT_INCONCLUSIVE;
            }
        }
    }
    
    @Override
    public final byte trainingNegative(Message message) {
        long startTime = System.currentTimeMillis();
        TreeSet<BinomialDistribution> bdSet = getDistributions(message);
        byte output;
        if (bdSet == null) {
            output = OUTPUT_INCONCLUSIVE;
        } else {
            output = getOutput(message, bdSet);
            if (output == OUTPUT_NEGATIVE) {
                addSuccessNegative();
                ajustNegativeThreshold();
            } else if (output == OUTPUT_POSITIVE) {
                addFailurePositive();
                ajustPositiveThreshold();
            }
            for (BinomialDistribution bd : bdSet) {
                bd.addFailure();
            }
        }
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
    
    @Override
    public final byte trainingPositive(Message message) {
        long startTime = System.currentTimeMillis();
        TreeSet<BinomialDistribution> bdSet = getDistributions(message);
        byte output;
        if (bdSet == null) {
            output = OUTPUT_INCONCLUSIVE;
        } else {
            output = getOutput(message, bdSet);
            if (output == OUTPUT_NEGATIVE) {
                addFailureNegative();
                ajustNegativeThreshold();
            } else if (output == OUTPUT_POSITIVE) {
                addSuccessPositive();
                ajustPositiveThreshold();
            }
            for (BinomialDistribution bd : bdSet) {
                bd.addSuccess();
            }
        }
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

    @Override
    public final byte classify(Message message) {
        long startTime = System.currentTimeMillis();
        byte output = getOutput(message, getDistributions(message));
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
