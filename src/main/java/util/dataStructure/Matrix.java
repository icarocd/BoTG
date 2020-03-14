package util.dataStructure;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.IntStream;
import util.DataStructureUtils;
import util.Logs;
import util.io.FileUtils;

public abstract class Matrix<T extends Comparable<? extends T>> {

	private final Class<T> type;

	public Matrix(Class<T> type) {
		this.type = type;
    }

	public abstract int getLineNumber();

	public abstract int getColumnNumber();

	public Class<T> getType() {
		return type;
	}

	public abstract T getValue(int i, int j);

	/**
	 * The values from line i, starting from 0.
	 * The returned collection SHOULD NOT BE CHANGED, otherwise it may affect the matrix.
	 * Default implementation returns a new array containing copies, although subclasses may freely override it to optime the function.
	 */
	protected Iterable<T> getValues(int i) {
		List<T> lineCopy = new ArrayList<>(getColumnNumber());
		for (int j = 0; j < getColumnNumber(); j++) {
			lineCopy.add(getValue(i, j));
		}
		return lineCopy;
	}

	public abstract void setValue(int i, int j, T value);

	public abstract boolean isParallelSupported();

	/**
	 * Get a submatrix.
	 * @param i0 Initial row index
	 * @param i1 Final row index
	 * @param j0 Initial column index
	 * @param j1 Final column index
	 */
	public Matrix<T> submatrix(int i0, int i1, int j0, int j1) {
		Matrix<T> X = new FixedMatrix<>(getType(), i1 - i0 + 1, j1 - j0 + 1);

		for (int i = i0; i <= i1; i++) {
			for (int j = j0; j <= j1; j++) {
				X.setValue(i - i0, j - j0, getValue(i, j));
			}
		}
		return X;
	}

	/** For line i, returns the sum of its values */
    public float getLineSum(int i) {
        Matrix<Float> this_ = (Matrix<Float>)this;
        float sum = 0;
        for(int j = 0; j < getColumnNumber(); j++)
            sum += this_.getValue(i, j);
        return sum;
    }

	/** For line i, and for each j indicated, returns the sum of all these cell values */
	public float getLineSum(int i, Iterable<Integer> js) {
		float sum = 0;
		for(int j : js)
			sum += (Float)getValue(i, j);
		return sum;
	}

	/** @return the sum for each line */
    public float[] getLineSums() {
        Matrix<Float> this_ = (Matrix<Float>) this;
        int nLines = getLineNumber();
        float[] lineSums = new float[nLines];
        int nColumns = getColumnNumber();
        IntStream forLines = IntStream.range(0, nLines);
        if (isParallelSupported())
            forLines = forLines.parallel();
        forLines.forEach(i -> {
            for (int j = 0; j < nColumns; j++)
                lineSums[i] += this_.getValue(i, j);
        });
        return lineSums;
    }

	/**
	 * Retorna o argmin por row: cada celula do array retornado corresponde a uma linha,
	 * e a celula da linha contem o indice da coluna da linha com o menor valor na linha.
	 */
	public int[] getArgminByLine(){
	    int lines = getLineNumber();

	    int[] result = new int[lines]; //cada indice contem, por linha, o indice da coluna de menor valor

	    // search for minimun
	    for(int i = 0; i < lines; i++){
	        result[i] = getArgminInLine(i);
	    }

	    return result;
	}

	public int getArgminInLine(final int lineIndex) {
		int argmin = -1;
		Comparable min = null;
		for (int j = 0; j < getColumnNumber(); j++) {
			Comparable value = getValue(lineIndex, j);
			if(min == null || value.compareTo(min) < 0){
				min = value;
				argmin = j;
			}
		}
		return argmin;
	}

	public void divideLineValues(int i, float divider) {
        Matrix<Float> this_ = (Matrix<Float>)this;
	    for (int j = 0; j < getColumnNumber(); j++) {
            this_.setValue(i, j, this_.getValue(i, j) / divider);
        }
	}

	/** Retrieves the Kth lowest value from line i, where both i and k starts in 1. */
	public T getKthLowestValueInLine(int i, int k) {
		Iterable line = getValues(i);
		return (T) DataStructureUtils.kthLowest(line, k);
	}

	/** Given the Kth lowest value from each line, where k starts in 1, returns the sum of these Kth lowest line values */
	public float getSumKthLowestValueInLines(int k) {
        if (!isParallelSupported()) {
		    float sumDistanceKNearestNeighbor = 0;
		    for (int i = 0; i < getLineNumber(); i++) {
		        sumDistanceKNearestNeighbor += (Float)getKthLowestValueInLine(i, k);
		    }
		    return sumDistanceKNearestNeighbor;
		}
        else {
            DoubleAdder sumDistanceKNearestNeighbor = new DoubleAdder();
            IntStream.range(0, getLineNumber()).parallel().forEach(i -> {
                sumDistanceKNearestNeighbor.add((Float)getKthLowestValueInLine(i, k));
            });
            return sumDistanceKNearestNeighbor.floatValue();
        }
	}

	/**
	 * For line i, return the column indices in which matrix[i,j] < limit.
	 */
	public ArrayList<Integer> getColumnIndicesOfValuesLowerThan(int i, float limit) {
        Matrix<Float> this_ = (Matrix<Float>) this;
		ArrayList<Integer> columnIndicesOfValuesLower = new ArrayList<>();
		for (int j = 0; j < getColumnNumber(); j++) {
			if(this_.getValue(i, j) < limit){
				columnIndicesOfValuesLower.add(j);
			}
		}
		return columnIndicesOfValuesLower;
	}

	/** Releases any kind of open resource, such as files etc */
	public void destroyResources(){}

	public synchronized void save(File file) {
		save(file, false, getLineNumber());
	}

	protected void save(File file, boolean startColumnIdxAfterLineIdx, int lines) {
		try (PrintStream stream = FileUtils.createPrintStreamToFile(file)) {
			boolean isDecimal = type.equals(Float.class) || type.equals(Double.class);

	    	DecimalFormat format = FileUtils.getDecimalFormatter();

	        for (int i = 0; i < lines; i++) {
	        	if(i % 20 == 0){
	        		Logs.finest("Saving matrix on " + file+". Now on line "+i+" of "+lines);
	        	}
	        	if (i > 0) {
	        		stream.println();
	        	}
	            final int startJ = startColumnIdxAfterLineIdx ? i + 1 : 0;
	            for (int j = startJ; j < getColumnNumber(); j++) {
	                if (j > startJ) {
	                    stream.print(' ');
	                }
	                T value = getValue(i, j);
					if(isDecimal){
						stream.print(format.format(value));
					}else{
						stream.print(value);
					}
	            }
	        }
		}
	}

	public static Matrix<Float> load(File file) throws IOException, ParseException {
        DecimalFormat format = FileUtils.getDecimalFormatter();

        List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
        int nLines = lines.size();
        Matrix<Float> m = null;
        for (int i = 0; i < nLines; i++) {
            String[] linePieces = lines.get(i).split(" ");
            if(i == 0){
                m = new FixedMatrix<>(Float.class, nLines, linePieces.length);
            }
            for (int j = 0; j < linePieces.length; j++) {
                float v = format.parse(linePieces[j]).floatValue();
                m.setValue(i, j, v);
            }
        }
        return m;
    }
}
