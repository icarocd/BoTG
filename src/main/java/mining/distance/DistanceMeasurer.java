package mining.distance;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import mining.Sample;
import util.Logs;
import util.QuintupleConsumer;
import util.dataStructure.SymmetricDistanceFlexibleMatrix;
import util.dataStructure.SymmetricDistanceFlexibleMatrixHashLong;

public interface DistanceMeasurer<T> {

	public abstract float getDistance(T sampleA, T sampleB);

	public default SymmetricDistanceFlexibleMatrix computeDistanceMatrix_indices(List<T> elements, boolean prioritizeMemoryInsteadOfSpeed, boolean parallel) {
		SymmetricDistanceFlexibleMatrix m = SymmetricDistanceFlexibleMatrix.create(elements.size(), prioritizeMemoryInsteadOfSpeed);
		computeDistanceMatrix_(elements, (i,j,elementI,elementJ,distance) -> m.setValue(i, j, distance), parallel);
		return m;
	}
	public default SymmetricDistanceFlexibleMatrixHashLong computeDistanceMatrix_ids(List<T> elements, boolean parallel) {
		SymmetricDistanceFlexibleMatrixHashLong m = new SymmetricDistanceFlexibleMatrixHashLong(elements.size());
		QuintupleConsumer<Integer,Integer,T,T,Float> consumer = (i,j,elementI,elementJ,distance) -> m.setValue(((Sample)elementI).getId(), ((Sample)elementJ).getId(), distance);
		computeDistanceMatrix_(elements, consumer, parallel);
		return m;
	}
	public default SymmetricDistanceFlexibleMatrixHashLong computeDistanceMatrix_ids(List<T> queryElements, List<T> responseElements, boolean parallel) {
        SymmetricDistanceFlexibleMatrixHashLong m = new SymmetricDistanceFlexibleMatrixHashLong(0);
        QuintupleConsumer<Integer,Integer,T,T,Float> consumer = (i,j,elementI,elementJ,distance) -> m.setValue(((Sample)elementI).getId(), ((Sample)elementJ).getId(), distance);
        computeDistanceMatrix_(queryElements, responseElements, consumer, parallel);
        return m;
    }
    /** compute distances considering queries and responses from the same collection */
	public default void computeDistanceMatrix_(List<T> elements, QuintupleConsumer<Integer,Integer,T,T,Float> consumer, boolean parallel) {
		final int numElements = elements.size();
	    final AtomicInteger progress = new AtomicInteger();
	    final int reportStep = (int) (numElements * 0.05); //every 5%

	    //OBS: SymmetricDistanceFlexibleMatrix ja devolve mesmos valores para entradas <i,j> e <j,i>. E 0 para <i,i>
	    IntStream queryIndexRange = IntStream.range(0, numElements);
	    if(parallel){
	    	queryIndexRange = queryIndexRange.parallel();
		}
		queryIndexRange.forEach(i -> {
			{
				int currentIdx = progress.incrementAndGet();
				if(currentIdx % reportStep == 0)
					Logs.finest("[computeDistanceMatrix] progress: now on sample " + currentIdx + " of " + numElements);
			}
		    T elementI = elements.get(i);
		    for (int j = i + 1; j < numElements; j++) {
		    	T elementJ = elements.get(j);
				float distance = getDistance(elementI, elementJ);
		    	consumer.accept(i, j, elementI, elementJ, distance);
		    }
		});
	}
	/** compute distances considering queries and responses from different collections */
    public default void computeDistanceMatrix_(List<T> queryElements, List<T> responseElements, QuintupleConsumer<Integer,Integer,T,T,Float> consumer, boolean parallel) {
        IntStream queryIndexRange = IntStream.range(0, queryElements.size());
        if(parallel){
            queryIndexRange = queryIndexRange.parallel();
        }
        queryIndexRange.forEach(i -> {
            T elementI = queryElements.get(i);
            for (int j = 0; j < responseElements.size(); j++) {
                T elementJ = responseElements.get(j);
                float distance = getDistance(elementI, elementJ);
                consumer.accept(i, j, elementI, elementJ, distance);
            }
        });
    }


	public static <T> SymmetricDistanceFlexibleMatrix computeDistanceMatrix_indices(List<T> elements, boolean prioritizeMemoryInsteadOfSpeed, boolean parallel, DistanceMeasurer<T> distanceMeasurer) {
		return distanceMeasurer.computeDistanceMatrix_indices(elements, prioritizeMemoryInsteadOfSpeed, parallel);
	}
}
