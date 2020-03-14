package mining.distance.graphDistance;

import mining.distance.SampleDistanceMeasurer;
import mining.textMining.textToGraph.model.GraphSample;

public class GraphSampleDistanceMeasurer extends SampleDistanceMeasurer<GraphSample> {

    private final GraphDistanceType graphDistanceType;

    public GraphSampleDistanceMeasurer(GraphDistanceType graphDistanceType) {
        this.graphDistanceType = graphDistanceType;
    }

    public GraphDistanceType getGraphDistanceType() {
        return graphDistanceType;
    }

    @Override
    public float getDistance(GraphSample sampleA, GraphSample sampleB) {
        return graphDistanceType.calculateDistance(sampleA.getGraph(), sampleB.getGraph());
    }

    @Override
    public float getSimilarity(GraphSample sampleA, GraphSample sampleB) {
        return graphDistanceType.calculateSimilarity(sampleA.getGraph(), sampleB.getGraph());
    }

    public String toString() {
    	return graphDistanceType.name();
    }
}
