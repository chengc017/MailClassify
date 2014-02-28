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
public class BinomialDistribution implements Comparable<BinomialDistribution> {
    
    private short population;
    
    private float failure;
    private float success;
    
    // Populational limit.
    private final short LIMIT;
    
    public BinomialDistribution() {
        population = 2;
        failure = 1.0f;
        success = 1.0f;
        LIMIT = 128;
    }
    
    public BinomialDistribution(float probability) {
        population = 1;
        failure = 0.0f;
        success = probability;
        LIMIT = 128;
    }
    
    public BinomialDistribution(int limit) {
        if (limit < 4) {
            limit = 4;
        } else if (limit > Short.MAX_VALUE) {
            limit = Short.MAX_VALUE - 1;
        } else if (limit % 2 != 0) {
            limit--;
        }
        population = 2;
        failure = 1.0f;
        success = 1.0f;
        LIMIT = (short) limit;
    }
    
    private BinomialDistribution(short population,
            float failure, float success, short limit) {
        this.population = population;
        this.failure = failure;
        this.success = success;
        this.LIMIT = limit;
    }
    
    public BinomialDistribution(ObjectInputStream ois) throws IOException, Exception {
        int version = ois.readInt();
        if (version == 1) {
            population = ois.readShort();
            failure = ois.readFloat();
            success = ois.readFloat();
            LIMIT = ois.readShort();
        } else {
            throw new Exception("Invalid version.");
        }
    }
    
    public void store(ObjectOutputStream oos) throws IOException {
        oos.writeInt(1); // Version
        oos.writeShort(population);
        oos.writeFloat(failure);
        oos.writeFloat(success);
        oos.writeShort(LIMIT);
    }
    
    // Downsize population to half.
    private void downsizePopulation() {
        population /= 2;
        failure /= 2;
        success /= 2;
    }
    
    public synchronized void addFailure() {
        population++;
        failure++;
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
    
    public float getFailureProbability() {
        return failure / population;
    }
    
    public short getPopulation() {
        return population;
    }
    
    private static NumberFormat FORMATTER =
            NumberFormat.getPercentInstance();
    
    @Override
    public String toString() {
        return FORMATTER.format(getSuccessProbability());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof BinomialDistribution) {
            BinomialDistribution other = (BinomialDistribution) o;
            float thisValue = this.getSuccessProbability();
            float otherValue = other.getSuccessProbability();
            return thisValue == otherValue;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (int) (getSuccessProbability() * Integer.MAX_VALUE);
    }

    @Override
    public int compareTo(BinomialDistribution other) {
        float thisValue = this.getSuccessProbability();
        float otherValue = other.getSuccessProbability();
        if (thisValue < otherValue) {
            return -1;
        } else if (thisValue > otherValue) {
            return 1;
        } else {
            return 0;
        }
    }
}
