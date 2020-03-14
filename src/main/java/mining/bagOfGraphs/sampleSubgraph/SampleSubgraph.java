package mining.bagOfGraphs.sampleSubgraph;

import util.graph.MeasurableGraph;

public abstract class SampleSubgraph implements MeasurableGraph {

    public abstract float calculateDistance(SampleSubgraph sampleSubgraph);
}
