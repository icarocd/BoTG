package mining.bagOfGraphs;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.google.common.collect.MinMaxPriorityQueue;
import mining.bagOfGraphs.sampleSubgraph.SubgraphsHandler;
import mining.bagOfGraphs.sampleSubgraph.linkedElement.LinkedElement;
import mining.textMining.textToGraph.GraphDatasetStats;
import util.DataStructureUtils;
import util.Pair;
import util.StringUtils;

/**
 * Codebook generator that createby selects subgraphs with best frequency criteria
 */
public class CodebookGeneratorFrequency extends CodebookGenerator {

    private final String approach;

    public CodebookGeneratorFrequency(boolean onlyComplexSubgraphsForCodebookSet, int maxElementsBySampleForCodebookSet, String approach) {
        super(onlyComplexSubgraphsForCodebookSet, maxElementsBySampleForCodebookSet, false, -1);
        //PS: here we tell the super class not to filter the codebookSet, because we do it here manually...
        this.approach = approach;
    }

    @Override
    public void generate(SubgraphsHandler subgraphsHandler, GraphDatasetStats stats, File codebookSetFile, File outputFile) {
        if("1".equals(approach))
            approach1(subgraphsHandler, stats, codebookSetFile, outputFile);
        else if("2".equals(approach)) //gera um vocabulario maior que na abordagem 1, mantendo mais de um subgrafo por mesmo termo central desde que possuam complementariedade entre si, ao codebook
            filterCodebookSet(subgraphsHandler, stats, codebookSetFile, outputFile);
        else if(approach.matches("top\\d+"))
            approachTopX(subgraphsHandler, stats, codebookSetFile, outputFile);
        else throw new IllegalArgumentException("unrecognized approach: "+approach);
    }

    //seleciona, por termo, o melhor subgrafo possivel (que maximiza a qtd de edges de DF > 1):
    private void approach1(SubgraphsHandler subgraphsHandler, GraphDatasetStats stats, File codebookSetFile, File outputFile) {
        Map<String,LinkedElement> bestByCentralTerm = new TreeMap<>();
        subgraphsHandler.loadSamplesSubgraphs(codebookSetFile, maxElementsToUseFromCodebookSet, subgraph -> {
            LinkedElement s = (LinkedElement)subgraph;
            if(stats.getTermDF(s.getElement()) > 1){ //subgrafo de termo central nao recorrente nao é representativo
                String centralTerm = s.getElement();
                LinkedElement best = bestByCentralTerm.get(centralTerm);
                if(best == null){
                    bestByCentralTerm.put(centralTerm, s);
                }else{
                    int score = s.getCountEdgesDFsBiggerThanOne(stats);
                    int scoreBest = best.getCountEdgesDFsBiggerThanOne(stats);
                    //quanto maior a qtd de arestas recorrentes na coleção, mais representativo é o subgrafo
                    //havendo empate se score, preferir o subgrafo mais simples pois as arestas remanescentes (de DF unário) não são úteis
                    if( score > scoreBest || (score == scoreBest && s.getNumEdges() < best.getNumEdges()) ){
                        bestByCentralTerm.put(centralTerm, s);
                    }
                }
            }
        });
        export(bestByCentralTerm.values().stream(), subgraphsHandler, outputFile);
    }

    //gera um vocabulario contendo os top-X subgrafos de maior pesagem de termo central (exemplo: top-X de TF-IDF)
    private void approachTopX(SubgraphsHandler subgraphsHandler, GraphDatasetStats stats, File codebookSetFile, File outputFile) {
        int limit = Integer.parseInt(StringUtils.retainDigits(approach));

        //pre-filtra bons candidatos:
        filterCodebookSet(subgraphsHandler, stats, codebookSetFile, codebookSetFile);

        MinMaxPriorityQueue<Pair<LinkedElement,Float>> codebookTemp = MinMaxPriorityQueue
            .orderedBy(Pair.<LinkedElement,Float>createComparatorByBReversed())
            .expectedSize(limit).maximumSize(limit).create();
        subgraphsHandler.loadSamplesSubgraphs(codebookSetFile, maxElementsToUseFromCodebookSet, subgraph -> {
            LinkedElement s = (LinkedElement)subgraph;
            codebookTemp.add(Pair.get(s, s.getElementWeight()));
        });

        List<LinkedElement> c = DataStructureUtils.consumeQueueToList(codebookTemp, pair -> pair.getA());
        export(c.stream(), subgraphsHandler, outputFile);
    }

    @Override
    public boolean isDatasetStatsRequired() {
        return true;
    }
}
