package util;

import java.util.function.BiConsumer;

public interface FloatVector {

    public abstract int length();

    public abstract int getNumNonZeroValues();

    public abstract float get(int i);

    public abstract void set(int i, float v);

    public abstract void add(int i, float v);

    public abstract void divideBy(int i, int divisor);

    public abstract float sum();

    public abstract double squareSum();

    public abstract double norm();

    /**
     * Provides an abstract way to iterate over each <index,value> containing non-zero values within the vector
     */
    public abstract void forEachNonZero(BiConsumer<Integer,Float> task);

    public default float[] values(){
        float[] values = new float[length()];
        for (int i = 0; i < values.length; i++) {
            values[i] = get(i);
        }
        return values;
    }
}
