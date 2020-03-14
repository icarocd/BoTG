package util.dataStructure;

import java.io.File;
import java.io.PrintStream;
import java.util.Scanner;
import util.TriConsumer;
import util.io.FileUtils;

/**
 * Matriz de distancias simetrica, cuja utilização de memória cresce conforme demanda.
 *
 * It assumes that:
 *    distance(A,A) = 0
 *    distance(A,B) = distance(B,A)
 *    distance(A,B) belongs to [0,1]
 * It avoids the storage of distances for pairs whose distance is 1, i.e. each pair not yet stored is assumed to have distance 1.
 */
public abstract class SymmetricDistanceFlexibleMatrix extends Matrix<Float> {

	protected final int numElements;

	public SymmetricDistanceFlexibleMatrix(int numElements) {
    	super(Float.class);
    	this.numElements = numElements;
    }

	public int getLineNumber() {
		return numElements;
	}

	public int getColumnNumber() {
		return getLineNumber();
	}

	public final Float getValue(int i, int j) {
	    if(i == j) //distance from an element to itself is considered 0
	    	return 0F;
	    Float v;
	    if(i < j)
	    	v = getInternal(i, j);
	    else
	    	v = getInternal(j, i);
	    return v != null ? v : 1F;
	}

	public final void setValue(int id1, int id2, Float distance) {
		if(distance == 1F) //we don't spend space to store 1 since it is the most common value for distances
			return;
		if(id1 == id2) //distance from an element to itself is considered 0, so we avoid spending space in such cases
			return;
		if(distance < 0F || distance > 1F)
			throw new UnsupportedOperationException("distances out of [0,1] are not currently supported. Parameters used: d("+id1+","+id2+")="+distance);
		if(id1 < id2)
			putInternal(id1, id2, distance);
		else
			putInternal(id2, id1, distance);
	}

	protected abstract Float getInternal(int id1, int id2);

	protected abstract void putInternal(int id1, int id2, float distance);

	/**
	 * WARNING: do NOT provide pairs <A,B> where:
	 *    A=B, because in such cases distances are always considered 0
	 *    d(A,B)=1, because these pairs are not stored internally (by convention missing pairs are treated as distance 1)
	 *    A>B, because for all pairs that presents A<B are already provided
	 */
	protected abstract void forAllNonSparseDistances(TriConsumer<Integer,Integer,Float> distanceConsumer);

	public void saveCompact(File file) {
        try (PrintStream stream = FileUtils.createPrintStreamToFile(file)) {
            saveCompact(stream);
        }
    }
	public void saveCompact(PrintStream stream) {
	    stream.append(String.valueOf(numElements)).append('\n');
	    forAllNonSparseDistances((id1, id2, distance) -> {
	        stream.append(String.valueOf(id1)).append(' ').append(String.valueOf(id2)).append(' ').append(String.valueOf(distance)).append('\n');;
	    });
	}
    public static SymmetricDistanceFlexibleMatrix loadCompact(File file, boolean prioritizeMemoryInsteadOfSpeed) {
        try(Scanner reader = FileUtils.createScannerFromFile(file)){
            return loadCompact(reader, prioritizeMemoryInsteadOfSpeed);
        }
    }
    public static SymmetricDistanceFlexibleMatrix loadCompact(Scanner reader, boolean prioritizeMemoryInsteadOfSpeed) {
        int numElements = reader.nextInt();
        SymmetricDistanceFlexibleMatrix matrix = create(numElements, prioritizeMemoryInsteadOfSpeed);
        while(reader.hasNext()){
        	int id1 = reader.nextInt();
            int id2 = reader.nextInt();
            float distance = Float.parseFloat(reader.next());
            matrix.putInternal(id1, id2, distance);
        }
        return matrix;
    }

    public static SymmetricDistanceFlexibleMatrix create(int numElements, boolean prioritizeMemoryInsteadOfSpeed) {
		return prioritizeMemoryInsteadOfSpeed ? new SymmetricDistanceFlexibleMatrixCompact(numElements) : new SymmetricDistanceFlexibleMatrixHash(numElements);
	}
}
