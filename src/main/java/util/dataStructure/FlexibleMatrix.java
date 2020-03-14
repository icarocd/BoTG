package util.dataStructure;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import util.MathUtils;
import util.TriConsumer;

/**
 * Matrix that holds only the indices that were set and whose value is != 0. For all other cases, this implemention does not allocate memory and considers value 0.
 */
public class FlexibleMatrix extends Matrix<Float> {

    private final Map<Integer, Map<Integer, Float>> values;
    private final int lines;
    private final int columns;

    public FlexibleMatrix(int lines, int columns) {
        super(Float.class);
        values = new TreeMap<>();
        this.lines = lines;
        this.columns = columns;
    }

    @Override
    public int getLineNumber() {
        return lines;
    }

    @Override
    public int getColumnNumber() {
        return columns;
    }

    public Float getValue(int i, int j) {
        //assertValidRowIndex(i);
        //assertValidColumnIndex(j);

        Map<Integer, Float> line = getLine(i);
        if (line != null) {
            Float value = line.get(j);
            if (value != null)
                return value;
        }
        return 0F;
    }

	@Override
    public void setValue(int i, int j, Float value) {
        //assertValidRowIndex(i);
        //assertValidColumnIndex(j);

        if(Objects.equals(0F, value)){
            //nao precisamos gastar memoria guardando valor que coincida com o default para a matriz!
            //MAS precisamos remover o valor anterior associado ao indice se existir (que será diferente do default)
            Map<Integer,Float> l = getLine(i);
            if(l != null)
                l.remove(j);
            return;
        }

        Map<Integer,Float> line = getOrCreateLine(i);

        line.put(j, value);
    }

    private Map<Integer, Float> getOrCreateLine(int i) {
        Map<Integer, Float> line = getLine(i);
        if (line == null) {
            line = new TreeMap<>();
            values.put(i, line);
        }
        return line;
    }

	@Override
    public boolean isParallelSupported() {
        return false; //TODO melhoria: tornar paralelizavel (SE vier a ser usado)
    }

	@Override
	protected Iterable<Float> getValues(int i) {
	    return values.get(i).values();
	}

	@Override
	public float getLineSum(int i) {
	    Map<Integer,Float> line = getLine(i);
	    if(line == null)
	        return 0F;
        return MathUtils.sumFloat(line.values());
	}

	@Override
	public void divideLineValues(int i, float divider) {
	    Map<Integer,Float> line = getLine(i);
        if(line != null){
            for(Entry<Integer,Float> j_and_value : line.entrySet()){
                j_and_value.setValue(j_and_value.getValue().floatValue() / divider);
            }
        }
	}

    public Map<Integer,Float> getLine(int i) {
        return values.get(i);
    }

    protected void assertValidRowIndex(int i) {
        if(i < 0 || i >= getLineNumber())
            throw new IndexOutOfBoundsException();
    }

    protected void assertValidColumnIndex(int j) {
        if(j < 0 || j >= getColumnNumber())
            throw new IndexOutOfBoundsException();
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public void forEachNonZero(TriConsumer<Integer,Integer,Float> consumer) {
        for(Entry<Integer,Map<Integer,Float>> lineIdx_line : values.entrySet()){
            Integer i = lineIdx_line.getKey();
            Map<Integer,Float> line_columns = lineIdx_line.getValue();
            for(Entry<Integer,Float> columnIdx_value : line_columns.entrySet()){
                consumer.accept(i, columnIdx_value.getKey(), columnIdx_value.getValue());
            }
        }
    }
}
