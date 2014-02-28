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
public class NormalDistribution {
    
    private short population;
    
    private float xiSum;
    private double xi2Sum;
    
    // Populational limit.
    private final short LIMIT;
    
    public NormalDistribution() {
        population = 0;
        xiSum = 0.0f;
        xi2Sum = 0.0f;
        LIMIT = 128;
    }
    
    public NormalDistribution(int limit) {
        if (limit < 4) {
            limit = 4;
        } else if (limit > Short.MAX_VALUE) {
            limit = Short.MAX_VALUE - 1;
        } else if (limit % 2 != 0) {
            limit--;
        }
        population = 0;
        xiSum = 0.0f;
        xi2Sum = 0.0f;
        LIMIT = (short) limit;
    }
    
    public NormalDistribution(ObjectInputStream ois) throws IOException {
        population = ois.readShort();
        xiSum = ois.readFloat();
        xi2Sum = ois.readDouble();
        LIMIT = ois.readShort();
    }
    
    public void store(ObjectOutputStream oos) throws IOException {
        oos.writeShort(population);
        oos.writeFloat(xiSum);
        oos.writeDouble(xi2Sum);
        oos.writeShort(LIMIT);
    }
    
    // Downsize population to half.
    private void downsizePopulation() {
        population /= 2;
        xiSum /= 2;
        xi2Sum /= 2;
    }
    
    public synchronized void addElement(float value) {
        population++;
        xiSum += value;
        xi2Sum += value * value;
        if (population == LIMIT) {
            downsizePopulation();
        }
    }
    
    public float getAverage() {
        return xiSum / population;
    }
    
    public double getStandardDeviation() {
        float avg = xiSum / population;
        double std = xi2Sum;
        std -= 2 * avg * xiSum;
        std += population * avg * avg;
        std /= population - 1;
        return Math.sqrt(std);
    }
    
    public double getStandardError() {
        return getStandardDeviation() / Math.sqrt(population);
    }
    
    private static NumberFormat numberFormat =
            NumberFormat.getNumberInstance();
    
    @Override
    public String toString() {
        if (population == 0) {
            return null;
        } else if (population == 1) {
            return numberFormat.format(getAverage());
        } else {
            return numberFormat.format(getAverage()) +
                    "±" + numberFormat.format(getStandardError());
        }
    }
}
