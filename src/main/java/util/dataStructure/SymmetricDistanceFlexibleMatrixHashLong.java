package util.dataStructure;

import java.io.File;
import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import util.Pair;
import util.TriConsumer;
import util.io.FileUtils;

public class SymmetricDistanceFlexibleMatrixHashLong {

	protected final int numElements;
    private final ConcurrentHashMap<Pair<Long,Long>, Float> distances;

    public SymmetricDistanceFlexibleMatrixHashLong(int numElements) {
        this.numElements = numElements;
        distances = new ConcurrentHashMap<>();
    }

    public boolean isParallelSupported() {
        return true;
    }

    public int getLineNumber() {
		return numElements;
	}

	public int getColumnNumber() {
		return getLineNumber();
	}

	public final Float getValue(long i, long j) {
	    if(i == j) //distance from an element to itself is considered 0
	    	return 0F;
	    Float v;
	    if(i < j)
	    	v = getInternal(i, j);
	    else
	    	v = getInternal(j, i);
	    return v != null ? v : 1F;
	}

    public final void setValue(long i, long j, Float distance) {
		if(distance == 1F) //we don't spend space to store 1 since it is the most common value for distances
			return;
		if(i == j) //distance from an element to itself is considered 0, so we avoid spending space in such cases
			return;
		if(distance < 0F || distance > 1F)
			throw new UnsupportedOperationException("distances out of [0,1] are not currently supported. Parameters used: d("+i+","+j+")="+distance);
		if(i < j)
			putInternal(i, j, distance);
		else
			putInternal(j, i, distance);
	}

    protected Float getInternal(long i, long j) {
        return distances.get(new Pair<>(i,j));
    }

    protected void putInternal(long i, long j, float distance) {
        distances.put(new Pair<>(i,j), distance);
    }

    /**
     * WARNING: do NOT provide pairs <A,B> where:
     *    A=B, because in such cases distances are always considered 0
     *    d(A,B)=1, because these pairs are not stored internally (by convention missing pairs are treated as distance 1)
     *    A>B, because for all pairs that presents A<B are already provided
     */
    protected void forAllNonSparseDistances(TriConsumer<Long, Long, Float> distanceConsumer) {
        for(Entry<Pair<Long,Long>,Float> pairAndDistance : distances.entrySet()){
            Pair<Long,Long> pair = pairAndDistance.getKey();
            distanceConsumer.accept(pair.getA(), pair.getB(), pairAndDistance.getValue());
        }
    }

	public void saveCompact(File file) {
		FileUtils.mkDirsForFile(file);
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
    public static SymmetricDistanceFlexibleMatrixHashLong loadCompact(File file) {
        try(Scanner reader = FileUtils.createScannerFromFile(file)){
            return loadCompact(reader);
        }
    }
    public static SymmetricDistanceFlexibleMatrixHashLong loadCompact(Scanner reader) {
        int numElements = reader.nextInt();
        SymmetricDistanceFlexibleMatrixHashLong matrix = new SymmetricDistanceFlexibleMatrixHashLong(numElements);
        while(reader.hasNext()){
        	long i = reader.nextLong();
        	long j = reader.nextLong();
            float distance = Float.parseFloat(reader.next());
            matrix.putInternal(i, j, distance);
        }
        return matrix;
    }
}
