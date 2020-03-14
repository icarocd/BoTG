package util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class FloatVectorSparse implements FloatVector {

    private Map<Integer,Float> values;
    private final int length; //TODO retirar daqui (pra economizar memoria)! como todos os vetores do dataset tem (usualmente?) mesmas larguras, bastaria armazenar para o dataset

    public FloatVectorSparse(int length) {
        this.length = length;
        values = new HashMap<>();
    }

    public FloatVectorSparse(int length, Map<Integer,Float> values) {
    	this.length = length;
        this.values = values;
        for(Iterator<Entry<Integer,Float>> it = values.entrySet().iterator(); it.hasNext(); ){
            Entry<Integer,Float> entry = it.next();
            if(entry.getValue().floatValue() == 0)
                it.remove();
        }
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public int getNumNonZeroValues() {
    	return values.size();
    }

    @Override
    public float get(int i) {
        Float v = values.get(i);
		if(v == null)
			return 0F;
		return v;
    }

    public Float getInternal(int i) {
        return values.get(i);
    }

    @Override
    public void set(int index, float weight) {
        if(weight != 0F){
            values.put(index, weight);
        }else{
            //nao precisamos gastar memoria guardando valor que coincida com o default!
            //MAS precisamos remover o valor anterior associado ao indice se existir (que será diferente do default)
            values.remove(index);
        }
    }

    @Override
    public synchronized void add(int i, float v) {
        if (v != 0F) {
            Float previous = values.get(i);
            if(previous != null){
            	values.put(i, previous + v);
            }else{
            	values.put(i, v);
            }
        }
    }

    @Override
    public synchronized void divideBy(int i, int divisor) {
        Float v = values.get(i);
        if (v != null) {
            values.put(i, v / divisor);
        }
    }

    @Override
    public synchronized float sum() {
        return MathUtils.sumFloat(values.values());
    }

    public synchronized float max() {
        Float max = MathUtils.max(values.values());
        if(max < 0) {
        	if(values.size() < length) //se ha menos entradas nao zero que o total, ha pelo menos uma entrada 0
        		return 0;
        }
		return max;
    }

    @Override
    public double squareSum() {
        return MathUtils.squareSum(values.values());
    }

    @Override
    public synchronized double norm() {
    	return MathUtils.norm(values.values());
    }

    @Override
    public void forEachNonZero(BiConsumer<Integer,Float> task) {
        if(values.isEmpty())
            return;
        //IMPORTANTE: obtem os indices em ordem, antes de iterar, e alem disso como criamos uma copia é possivel alterarmos os valores durante a iteração!
        TreeMap<Integer,Float> entriesInOrder = new TreeMap<>(values);
        for (Entry<Integer,Float> entry : entriesInOrder.entrySet()) {
            task.accept(entry.getKey(), entry.getValue());
        }
    }

    public void forceOrder() {
    	values = new TreeMap<>(values);
    }

    Map<Integer, Float> getValues() {
		return values;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
