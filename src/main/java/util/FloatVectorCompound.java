package util;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

class FloatVectorCompound implements FloatVector {

    private FloatVector v1, v2;

    public FloatVectorCompound(FloatVector v1, FloatVector v2){
        this.v1 = v1;
        this.v2 = v2;
    }

    @Override
    public int length() {
        return v1.length() + v2.length();
    }

    @Override
    public int getNumNonZeroValues() {
    	return v1.getNumNonZeroValues() + v2.getNumNonZeroValues();
    }

    @Override
    public float get(int i) {
        int l1 = v1.length();
        return i < l1 ? v1.get(i) : v2.get(i - l1);
    }

    @Override
    public void set(int i, float v) {
        int l1 = v1.length();
        if(i < l1)
            v1.set(i, v);
        else
            v2.set(i - l1, v);
    }

    @Override
    public void add(int i, float v) {
        int l1 = v1.length();
        if(i < l1)
            v1.add(i, v);
        else
            v2.add(i - l1, v);
    }

    @Override
    public void divideBy(int i, int divisor) {
        int l1 = v1.length();
        if(i < l1)
            v1.divideBy(i, divisor);
        else
            v2.divideBy(i - l1, divisor);
    }

    @Override
    public float sum() {
        return v1.sum() + v2.sum();
    }

    @Override
    public double squareSum() {
        return v1.squareSum() + v2.squareSum();
    }

    @Override
    public double norm() {
        return Math.sqrt(squareSum());
    }

    @Override
    public void forEachNonZero(BiConsumer<Integer,Float> task) {
        int l1 = v1.length();
        v1.forEachNonZero(task);
        v2.forEachNonZero((idx2, value) -> task.accept(idx2 + l1, value));
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
