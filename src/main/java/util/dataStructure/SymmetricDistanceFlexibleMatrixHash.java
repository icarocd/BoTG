package util.dataStructure;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import util.Pair;
import util.TriConsumer;

class SymmetricDistanceFlexibleMatrixHash extends SymmetricDistanceFlexibleMatrix {

    private final ConcurrentHashMap<Pair<Integer,Integer>, Float> distances;

    public SymmetricDistanceFlexibleMatrixHash(int numElements) {
        super(numElements);
        distances = new ConcurrentHashMap<>();
    }

    public boolean isParallelSupported() {
        return true;
    }

    protected Float getInternal(int id1, int id2) {
        return distances.get(new Pair<>(id1,id2));
    }

    protected void putInternal(int id1, int id2, float distance) {
        distances.put(new Pair<>(id1,id2), distance);
    }

    protected void forAllNonSparseDistances(TriConsumer<Integer, Integer, Float> distanceConsumer) {
        for(Entry<Pair<Integer,Integer>,Float> pairAndDistance : distances.entrySet()){
            Pair<Integer,Integer> pair = pairAndDistance.getKey();
            distanceConsumer.accept(pair.getA(), pair.getB(), pairAndDistance.getValue());
        }
    }

    public int hashCode() {
        return new HashCodeBuilder().append(numElements).append(distances).toHashCode();
    }

    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null || !getClass().equals(obj.getClass()))
            return false;
        SymmetricDistanceFlexibleMatrixHash other = (SymmetricDistanceFlexibleMatrixHash)obj;
        return new EqualsBuilder().append(numElements, other.numElements).append(distances, other.distances).isEquals();
    }

    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).append(numElements).append(distances).toString();
    }
}
