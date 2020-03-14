package util.dataStructure;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import util.dataStructure.Matrix;

public class FixedMatrix<T extends Comparable<? extends T>> extends Matrix<T> {

    private final T[][] values;

	public FixedMatrix(Class<T> type, T[][] content) {
		super(type);
        values = content;
    }

	public FixedMatrix(Class<T> type, int lines, int columns) {
		this(type, (T[][]) Array.newInstance(type, lines, columns));
    }

	@Override
	public int getLineNumber() {
		return values.length;
	}

	@Override
	public int getColumnNumber() {
		return values[0].length;
	}

	public T getValue(int i, int j) {
		return values[i][j];
	}

	@Override
	public void setValue(int i, int j, T value) {
		values[i][j] = value;
    }

	@Override
	public boolean isParallelSupported() {
	    return true;
	}

	@Override
	protected List<T> getValues(int i) {
		return Arrays.asList(values[i]);
	}
}
