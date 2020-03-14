package mining.distance;

import mining.distance.graphDistance.GraphDistanceType;
import mining.distance.graphDistance.GraphSampleDistanceMeasurer;

public abstract class SampleDistanceMeasurer<T> implements DistanceMeasurer<T> {

	public abstract float getSimilarity(T sampleA, T sampleB);

	public String toString() {
		return getName();
	}

	public String getName() {
        if(this instanceof CosineVectorSampleDistanceMeasurer)
            return "cosine";
        if(this instanceof JaccardVectorSampleDistanceMeasurer)
            return "Jaccard";
        if(this instanceof EuclideanVectorSampleDistanceMeasurer)
            return "Euclidean";
        if(this instanceof GraphSampleDistanceMeasurer){
            GraphDistanceType type = ((GraphSampleDistanceMeasurer)this).getGraphDistanceType();
            if(GraphDistanceType.MCS == type)
                return "MCS";
            if(GraphDistanceType.WGU == type)
                return "WGU";
            throw new IllegalArgumentException("not implemented yet for: " + type);
        }
        throw new IllegalArgumentException("not implemented yet for: " + getClass().getName());
    }
	public static <T> SampleDistanceMeasurer<T> get(String name) {
		if ("cosine".equalsIgnoreCase(name))
			return (SampleDistanceMeasurer<T>) new CosineVectorSampleDistanceMeasurer();
		if ("Jaccard".equalsIgnoreCase(name))
			return (SampleDistanceMeasurer<T>) new JaccardVectorSampleDistanceMeasurer();
		if ("Euclidean".equalsIgnoreCase(name))
			return (SampleDistanceMeasurer<T>) new EuclideanVectorSampleDistanceMeasurer();
		if ("MCS".equalsIgnoreCase(name))
		    return (SampleDistanceMeasurer<T>) new GraphSampleDistanceMeasurer(GraphDistanceType.MCS);
		if ("WGU".equalsIgnoreCase(name))
            return (SampleDistanceMeasurer<T>) new GraphSampleDistanceMeasurer(GraphDistanceType.WGU);
		throw new IllegalArgumentException("unsupported type: " + name);
	}
}
