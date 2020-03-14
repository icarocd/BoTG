package util.dataStructure;

import java.io.File;
import org.apache.commons.io.LineIterator;
import util.Logs;
import util.MathUtils;
import util.io.FileUtils;

/**
 * Implementação de matrix simetrica de distancias, que armazena distancias de modo a
 * economizar espaço evitando guardar a diagonal principal (valores 0) e elementoss abaixo dela (serao considerados
 * como iguais àqueles espelhados pela diagonal principal)
 *
 * Example, for 4 elements (indices 0 to 3). The internal structure will be:
 * 1st line: array with distances from element 0 to 1 2 and 3
 * 2nd line: array with distances from element 1 to 2 and 3
 * 3th line: array with distances from element 2 to 3
 *
 * Distance(3,2) will be first considered as distance(2,3) before being retrieved or saved.
 */
public class SymmetricDistanceFixedMatrix extends Matrix<Float> {

	private Float[][] distances;

	private SymmetricDistanceFixedMatrix(Float[][] distances) {
		super(Float.class);
		this.distances = distances;
	}

	public SymmetricDistanceFixedMatrix(int numElements) {
	    super(Float.class);
		try {
		    distances = new Float[numElements - 1][];
		    for (int i = 0; i < distances.length; i++) {
		        distances[i] = new Float[distances.length - i];
		    }
        } catch (OutOfMemoryError e) {
            int requiredSpace = (numElements*numElements - numElements)/2;
            Logs.severe("Error while allocating space (~"+requiredSpace+" floats) for " + getClass().getSimpleName() + ", for " + numElements + " elements");
            throw e;
        }
	}

	@Override
	public int getLineNumber() {
		return distances.length + 1;
	}

	@Override
	public int getColumnNumber() {
		return getLineNumber();
	}

	@Override
	public Float getValue(int i, int j) {
		if(i == j)
			return 0F;
		if(i < j)
			return distances[i][getInternalColumnIdx(i,j)];
		//we only keep distance from one of the two [i,j] and [j,i]. We keep those for i < j.
		return distances[j][getInternalColumnIdx(j,i)];
	}

	private int getInternalColumnIdx(int lineIdx, int columnIdx) {
		return columnIdx - 1 - lineIdx;
	}

	@Override
	public void setValue(int i, int j, Float value) {
		if (i == j)
			return;
		if (i < j) {
			distances[i][getInternalColumnIdx(i, j)] = value;
		} else {
			//we only keep distance from one of the two [i,j] and [j,i]. We keep those for i < j.
			distances[j][getInternalColumnIdx(j, i)] = value;
		}
	}

	@Override
    public boolean isParallelSupported() {
        return true;
    }

	public void save(File file, boolean includeRedundantElements) {
		if(includeRedundantElements) {
			super.save(file);
		}else{
			save(file, true, distances.length);
		}
	}

	public static SymmetricDistanceFixedMatrix loadFromReducedFormatFile(File filepath) {
		LineIterator lineIterator = FileUtils.lineIteratorOfFile(filepath);

		String[] firstLineValues = lineIterator.nextLine().split("\\s+");
		int nSamples = firstLineValues.length + 1;
		int nRows = nSamples - 1;

		Float[][] dists = new Float[nRows][];
		dists[0] = MathUtils.asFloatArray(firstLineValues);

		//leitura da 2a linha em diante:
		for (int idx = 2; idx <= nRows; idx++) {
			dists[idx - 1] = MathUtils.asFloatArray(lineIterator.nextLine().split("\\s+"));
		}

		return new SymmetricDistanceFixedMatrix(dists);
	}
}
