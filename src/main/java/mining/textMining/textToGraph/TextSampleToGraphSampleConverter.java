package mining.textMining.textToGraph;

import java.util.List;
import mining.textMining.TextSample;
import mining.textMining.parsing.TextSection;
import mining.textMining.parsing.TokenExtractor;
import mining.textMining.parsing.TokenList;
import mining.textMining.textToGraph.model.GraphSample;
import mining.textMining.textToGraph.model.GraphType;
import util.Collector;
import util.graph.DirectedWeightedLabeledGraph;
import util.graph.LabeledWeightedEdge;

public class TextSampleToGraphSampleConverter implements Collector<TextSample> {

    private final TokenExtractor tokenExtractor;
    private final GraphType graphType;
    private final boolean forceReach;
    private final int maxReach;
    private final Collector<GraphSample> sampleCollector;

    public TextSampleToGraphSampleConverter(TokenExtractor tokenExtractor, GraphType graphType, boolean forceReach,
        int maxReach, Collector<GraphSample> graphSampleCollector)
    {
        this.tokenExtractor = tokenExtractor;
        this.graphType = graphType;
        this.forceReach = forceReach;
        this.maxReach = maxReach;
        this.sampleCollector = graphSampleCollector;
    }

    @Override
    public void collect(TextSample textSample) {
        List<TextSection> sections = TextSection.loadSections(tokenExtractor, textSample);

        if (!sections.isEmpty()) {
            DirectedWeightedLabeledGraph graph = createGraph(sections);
            if(graph != null){
                sampleCollector.collect(new GraphSample(textSample.getId(), textSample.getLabels(), graph));
            }
//          else{
//              Logs.fine("Sample "+ textSample.getId() +" discarded due no vertices created!");
//          }
        }
//      else {
//          Logs.finer("Sample "+ textSample.getId() +" discarded due no terms!");
//      }
    }

    private DirectedWeightedLabeledGraph createGraph(List<TextSection> sections) {
        boolean doWeighting = graphType.isCountingRequired();

		DirectedWeightedLabeledGraph graph = new DirectedWeightedLabeledGraph(doWeighting);

        //add vertexes:
		if (!addVertexes(graph, sections)) {
        	return null; //interrupts the graph construction when no vertices were added
        }

        //add edges:
        final int maxNeighborhoodReach = forceReach || graphType.isEnlargeNeighborhoodReach() ? maxReach : 1;
        for (TextSection section : sections) {
            String sectionId = section.getId();
            for (TokenList subSection : section.getSubSections()) {
                List<String> terms = subSection.getTokens();

                int nTerms = terms.size();
                for (int termIdx = 0; termIdx < nTerms; termIdx++) {
                    String term = terms.get(termIdx);
                    for (int neighborIdx = termIdx + 1, reach = 1; neighborIdx < nTerms && reach <= maxNeighborhoodReach; neighborIdx++, reach++) {
                        String nextTerm = terms.get(neighborIdx);
                        addEdge(graph, sectionId, reach, term, nextTerm);
                    }
                }
            }
        }

        return graph;
    }

    private boolean addVertexes(DirectedWeightedLabeledGraph graph, List<TextSection> sections) {
        boolean added = false;
    	for (TextSection section : sections) {
            for (TokenList subSection : section.getSubSections()) {
                for (String term : subSection.getTokens()) {
                    graph.addVertex(term);
                    added = true;
                }
            }
        }
    	return added;
    }

    /**
     * @return true se aresta adicionada/editada, false caso contrario.
     */
    private boolean addEdge(DirectedWeightedLabeledGraph graph, String sectionId, int neighborReach, String source, String target) {
        if(source.equals(target)) //nao permitimos loops
            return false;

        String label = graphType.getLabel(sectionId, neighborReach);

        LabeledWeightedEdge edge = graph.getEdge(source, target, label);

        if (!graphType.isCountingRequired()) {
            if (edge != null) { // repetido
                return false;
            }
            edge = graph.addEdge(source, target);
            edge.setLabel(label);
            return true;
        } else {
            if (edge == null) {
                edge = graph.addEdge(source, target);
                edge.setLabel(label);
                edge.setWeight(1);
            } else {
                edge.addWeight(1);
            }
            return true;
        }
    }
}
