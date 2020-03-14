package util.dataStructure;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import mikera.matrixx.impl.SparseRowMatrix;
import mikera.vectorz.AVector;
import mikera.vectorz.impl.ASparseVector;
import util.TriConsumer;

/**
 * Different from {@link SymmetricDistanceFlexibleMatrixHash}, this is optimized for lower memory,
 * although the access time is degradated, from ~O(1), to ~ O(lgn) or O(n).
 */
class SymmetricDistanceFlexibleMatrixCompact extends SymmetricDistanceFlexibleMatrix {

	//PS: here we store similarities instead of distances because ASparseRCMatrix API defaults to 0 for missing values, and our need here is to default to 1...
	private final SparseRowMatrix similarities;

	private final Lock readLock;
	private final Lock writeLock;

	public SymmetricDistanceFlexibleMatrixCompact(int numElements) {
		super(numElements);
		similarities = SparseRowMatrix.create(numElements, numElements);
		ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();
	}

	public boolean isParallelSupported() {
		return true;
	}

	protected Float getInternal(int id1, int id2) {
		readLock.lock();
		try {
			AVector row = similarities.unsafeGetVector(id1);
			if(row == null)
				return null;
			return 1F - (float)row.unsafeGet(id2);
		} finally {
			readLock.unlock();
		}
	}

	protected synchronized void putInternal(int id1, int id2, float distance) {
		writeLock.lock();
		try {
			similarities.set(id1, id2, 1F - distance);
		} finally {
			writeLock.unlock();
		}
	}

	protected void forAllNonSparseDistances(TriConsumer<Integer, Integer, Float> distanceConsumer) {
	    readLock.lock();
        try {
            for (int i = 0; i < numElements; i++) {
                ASparseVector row = (ASparseVector) similarities.getRow(i);
                for(int j : row.nonZeroIndices()){
                    float similarity = (float)row.get(j);
                    distanceConsumer.accept(i, j, 1F - similarity);
                }
            }
        } finally {
            readLock.unlock();
        }
	}

	public int hashCode() {
	    return new HashCodeBuilder().append(numElements).append(similarities).toHashCode();
	}

    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null || !getClass().equals(obj.getClass()))
            return false;
        SymmetricDistanceFlexibleMatrixCompact other = (SymmetricDistanceFlexibleMatrixCompact)obj;
        return new EqualsBuilder().append(numElements, other.numElements).append(similarities, other.similarities).isEquals();
    }

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).append(numElements).append(similarities).toString();
    }
}
