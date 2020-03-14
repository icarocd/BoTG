package mining.bagOfGraphs.sampleSubgraph.linkedElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import mining.bagOfGraphs.sampleSubgraph.SampleSubgraph;
import mining.bagOfGraphs.sampleSubgraph.SubgraphsHandler;
import mining.textMining.textToGraph.model.GraphSample;
import util.DataStructureUtils;
import util.MathUtils;
import util.graph.LabeledMeasurableGraph;
import util.graph.LabeledWeightedEdge;
import java.util.Set;

public class LinkedElementSubgraphsHandler extends SubgraphsHandler {

    private final boolean alsoConsidererIncomingEdges;

    public LinkedElementSubgraphsHandler(boolean alsoConsidererIncomingEdges) {
        this.alsoConsidererIncomingEdges = alsoConsidererIncomingEdges;
    }

    @Override
    public List<SampleSubgraph> extractSubgraphs(GraphSample sample) {
        List<SampleSubgraph> subgraphs = new ArrayList<>();
        LabeledMeasurableGraph graph = sample.getGraph();
        boolean requireNeighborNodeWeights = LinkedElementDistances.isNeighborNodeWeightsRequired();
        for(String vertex : graph.vertexSet())
            subgraphs.add(createSubgraph(vertex, graph, requireNeighborNodeWeights));
        return subgraphs;
    }

    private SampleSubgraph createSubgraph(String vertex, LabeledMeasurableGraph graph, boolean requireNeighborNodeWeights) {
        float vertexWeight = graph.getVertexWeight(vertex).floatValue();

        //extracts weights:
        Set<LabeledWeightedEdge> edges = alsoConsidererIncomingEdges ? graph.edgesOf(vertex) : graph.outgoingEdgesOf(vertex);
        Map<String,Float> edgesWeights = new LinkedHashMap<>(edges.size(), 1);
        Map<String,Float> neighborsWeights = requireNeighborNodeWeights ? new LinkedHashMap<>(edges.size(), 1) : null;
        for(LabeledWeightedEdge edge : edges){
            String neighborVertex = (String) edge.getTarget();
            float edgeWeight = (float) edge.getWeight();
            edgesWeights.put(neighborVertex, edgeWeight);
            if(requireNeighborNodeWeights)
                neighborsWeights.put(neighborVertex, graph.getVertexWeight(neighborVertex).floatValue());
        }

        return new LinkedElement(vertex, vertexWeight, edgesWeights, neighborsWeights);
    }

	@Override
	protected CharSequence getSubgraphAsStringLine(SampleSubgraph subgraph) {
	    boolean requireNeighborNodeWeights = LinkedElementDistances.isNeighborNodeWeightsRequired();

		LinkedElement s = (LinkedElement) subgraph;
		StringBuilder stringForm = new StringBuilder(s.getElement()).append(" ").append(s.elementWeight);
        for (Entry<String, Float> edgeElementAndWeight : s.edgesWeights.entrySet()) {
            String neighborElement = edgeElementAndWeight.getKey();
			stringForm.append(" ").append(neighborElement).append(" ").append(edgeElementAndWeight.getValue());
			if(requireNeighborNodeWeights)
			    stringForm.append(" ").append(s.neighborsWeights.get(neighborElement));
        }
        return stringForm;
	}
	@Override
	protected SampleSubgraph getSubgraphFromStringLine(String stringForm) {
	    boolean requireNeighborNodeWeights = LinkedElementDistances.isNeighborNodeWeightsRequired();

		String[] chunks = stringForm.trim().split("\\s+");
        int chunkIdx = 0;

        String element = chunks[chunkIdx++];
        float elementWeight = MathUtils.asFloat(chunks[chunkIdx++]);

        int nEdges = (chunks.length - chunkIdx) / (requireNeighborNodeWeights ? 3 : 2);
        Map<String, Float> edgesWeights = new LinkedHashMap<>(nEdges, 1);
        Map<String, Float> neighborsWeights = requireNeighborNodeWeights ? new LinkedHashMap<>(nEdges, 1) : null;

        while(chunkIdx < chunks.length) {
            String t = chunks[chunkIdx++];
			edgesWeights.put(t, MathUtils.asFloat(chunks[chunkIdx++]));
            if(requireNeighborNodeWeights)
                neighborsWeights.put(t, MathUtils.asFloat(chunks[chunkIdx++]));
        }

        return new LinkedElement(element, elementWeight, edgesWeights, neighborsWeights);
    }

	@Override
	public void retainComplexSubgraphs(List<SampleSubgraph> subgraphs) {
	    for (Iterator<SampleSubgraph> it = subgraphs.iterator(); it.hasNext();) {
            LinkedElement subgraph = (LinkedElement)it.next();
            if(subgraph.getNumEdges() == 0)
                it.remove();
        }
	}

	@Override
    public <T extends SampleSubgraph> List<T> reduceToMostImportant(List<T> subgraphs_, int maxElements) {
    	if(maxElements < 0 || subgraphs_.size() < maxElements)
    		return subgraphs_;
    	List<LinkedElement> subgraphs = (List<LinkedElement>)subgraphs_;
    	Collections.sort(subgraphs, LinkedElement.COMPARATOR_BY_VERTEX_WEIGHT);
    	subgraphs = DataStructureUtils.subListEnd(subgraphs, maxElements);
    	return (List<T>)subgraphs;
    }
}
