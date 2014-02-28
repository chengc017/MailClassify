/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.allchemistry.mailclassify.distribution;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;

/**
 *
 * @author Leandro Carlos Rodrigues <leandro.carlos.rodrigues@gmail.com>
 */
public class TrinomialDistribution implements Comparable<TrinomialDistribution> {
    
    private short population;
    
    private float failure;
    private float inconclusive;
    private float success;
    
    // Populational limit.
    private final short LIMIT;
    
    public TrinomialDistribution() {
        population = 1;
        failure = 0.0f;
        inconclusive = 1.0f;
        success = 0.0f;
        LIMIT = 128;
    }
    
    public TrinomialDistribution(ObjectInputStream ois) throws IOException {
        population = ois.readShort();
        failure = ois.readFloat();
        inconclusive = ois.readFloat();
        success = ois.readFloat();
        LIMIT = ois.readShort();
    }
    
    public void store(ObjectOutputStream oos) throws IOException {
        oos.writeShort(population);
        oos.writeFloat(failure);
        oos.writeFloat(inconclusive);
        oos.writeFloat(success);
        oos.writeShort(LIMIT);
    }
    
    public TrinomialDistribution(int limit) {
        if (limit < 4) {
            limit = 4;
        } else if (limit > Short.MAX_VALUE) {
            limit = Short.MAX_VALUE - 1;
        } else if (limit % 2 != 0) {
            limit--;
        }
        population = 1;
        failure = 0.0f;
        inconclusive = 1.0f;
        success = 0.0f;
        LIMIT = (short) limit;
    }
    
    // Downsize population to half.
    private void downsizePopulation() {
        population /= 2;
        failure /= 2;
        inconclusive /= 2;
        success /= 2;
    }
    
    public synchronized void addFailure() {
        population++;
        failure++;
        if (population == LIMIT) {
            downsizePopulation();
        }
    }
    
    public synchronized void addInconclusive() {
        population++;
        inconclusive++;
        if (population == LIMIT) {
            downsizePopulation();
        }
    }
    
    public synchronized void addSuccess() {
        population++;
        success++;
        if (population == LIMIT) {
            downsizePopulation();
        }
    }
    
    public float getSuccessProbability() {
        return success / population;
    }
    
    public float getInconclusiveProbability() {
        return inconclusive / population;
    }
    
    public float getConclusiveProbability() {
        return (failure + success) / population;
    }
    
    public float getFailureProbability() {
        return failure / population;
    }
    
    private static NumberFormat percentageFormat =
            NumberFormat.getPercentInstance();
    
    @Override
    public String toString() {
        return percentageFormat.format(getFailureProbability())
                + ";" + percentageFormat.format(getSuccessProbability());
    }

    @Override
    public int compareTo(TrinomialDistribution other) {
        double thisProbability = this.getSuccessProbability();
        double otherProbability = other.getSuccessProbability();
        if (thisProbability < otherProbability) {
            return -1;
        } else if (thisProbability > otherProbability) {
            return 1;
        } else {
            return 0;
        }
    }
}
