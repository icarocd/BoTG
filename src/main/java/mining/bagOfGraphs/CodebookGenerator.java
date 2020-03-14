package mining.bagOfGraphs;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.mutable.MutableInt;
import mining.bagOfGraphs.sampleSubgraph.SampleSubgraph;
import mining.bagOfGraphs.sampleSubgraph.SubgraphsHandler;
import mining.bagOfGraphs.sampleSubgraph.linkedElement.LinkedElement;
import mining.textMining.textToGraph.GraphDatasetStats;
import util.DataStructureUtils;
import util.Logs;
import util.io.FileUtils;

public abstract class CodebookGenerator {

    private final boolean onlyComplexSubgraphsForCodebookSet;
    private final int maxElementsBySampleForCodebookSet;
    private final boolean filterCodebookSet;
	protected final int maxElementsToUseFromCodebookSet;

	/**
	 * @param maxElementsBySampleForCodebookSet -1 to use all.
	 * @param maxElementsToUseFromCodebookSet -1 to use all.
	 */
    public CodebookGenerator(boolean onlyComplexSubgraphsForCodebookSet, int maxElementsBySampleForCodebookSet, boolean filterCodebookSet, int maxElementsToUseFromCodebookSet) {
    	this.onlyComplexSubgraphsForCodebookSet = onlyComplexSubgraphsForCodebookSet;
        this.maxElementsBySampleForCodebookSet = maxElementsBySampleForCodebookSet;
        this.filterCodebookSet = filterCodebookSet;
    	this.maxElementsToUseFromCodebookSet = maxElementsToUseFromCodebookSet;
	}

	public void generate(SubgraphsHandler subgraphsHandler, GraphDatasetStats stats, Iterable<File> subgraphsFiles, File codebookFile) {
    	File codebookSetFile = getCodebookSetFile(codebookFile);
	    createCodebookCandidates(subgraphsHandler, stats, subgraphsFiles, codebookSetFile);

	    generate(subgraphsHandler, stats, codebookSetFile, codebookFile);

        FileUtils.deleteQuietly(codebookSetFile);
    }

    public File getCodebookSetFile(File codebookFile) {
    	 return new File(codebookFile.getParentFile(), codebookFile.getName() + "_codebookSet");
	}

	private void createCodebookCandidates(SubgraphsHandler subgraphsHandler, GraphDatasetStats stats, Iterable<File> subgraphsFiles, File outputFile) {
	    Logs.finest("Generating codebookSet");
        MutableInt codebookSetSize = new MutableInt(0);
        try( Writer codebookSetFileWriter = FileUtils.createWriterToFile(outputFile) ){
            if (onlyComplexSubgraphsForCodebookSet || maxElementsBySampleForCodebookSet > 0) { //in these cases, we need to inspect the subgraphs...
                long qtd = 0;
                for (File sampleSubgraphsFile : subgraphsFiles) {
                    if (++qtd % 2000 == 0) Logs.finest("[CodebookGenerator#createCodebookCandidates] now on " + qtd + "th sample");
                    List<SampleSubgraph> subgraphs = subgraphsHandler.loadSamplesSubgraphs(sampleSubgraphsFile, -1);
                    if (onlyComplexSubgraphsForCodebookSet)
                        subgraphsHandler.retainComplexSubgraphs(subgraphs);
                    subgraphs = subgraphsHandler.reduceToMostImportant(subgraphs, maxElementsBySampleForCodebookSet);
                    for (SampleSubgraph sampleSubgraph : subgraphs) {
                        subgraphsHandler.append(sampleSubgraph, codebookSetFileWriter);
                        codebookSetSize.increment();
                    }
                }
            } else {
                long qtd = 0;
                for(File sampleSubgraphsFile : subgraphsFiles){
                    if(++qtd % 2000 == 0){ Logs.finest("[CodebookGenerator#createCodebookCandidates] now on "+qtd+"th sample"); }
                    LineIterator lineIterator = FileUtils.lineIteratorOfFile(sampleSubgraphsFile);
                    while (lineIterator.hasNext()) {
                        String sampleSubgraphAsString = lineIterator.next();
                        if (!sampleSubgraphAsString.isEmpty()) {
                            subgraphsHandler.append(sampleSubgraphAsString, codebookSetFileWriter);
                            codebookSetSize.increment();
                        }
                    }
                    lineIterator.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Logs.info("#codebookSet: " + codebookSetSize);

        if(filterCodebookSet)
            filterCodebookSet(subgraphsHandler, stats, outputFile, outputFile);

        reduceCodebookSet(outputFile, codebookSetSize.intValue());
    }

    protected void filterCodebookSet(SubgraphsHandler subgraphsHandler, GraphDatasetStats stats, File codebookSetFile, File outputFile) {
        Logs.fine("filtering codebookSet");
        Map<String,List<LinkedElement>> bestByCentralTerm = new TreeMap<>();
        subgraphsHandler.loadSamplesSubgraphs(codebookSetFile, maxElementsToUseFromCodebookSet, subgraph -> analyze(stats, bestByCentralTerm, (LinkedElement)subgraph));
        Collection<List<LinkedElement>> codebookLists = bestByCentralTerm.values();
        if(codebookSetFile.equals(outputFile))
            outputFile.delete();
        export(DataStructureUtils.flattenCollectionOfLists(codebookLists), subgraphsHandler, outputFile);
    }
    /**
     * @return notAdd = 0, add = 1, replace = 2
     */
    int analyze(GraphDatasetStats stats, Map<String,List<LinkedElement>> bestByCentralTerm, LinkedElement s) {
        if(stats.getTermDF(s.getElement()) > 1){ //subgrafo de termo central nao recorrente nao é representativo
            String centralTerm = s.getElement();
            List<LinkedElement> bestSet = bestByCentralTerm.get(centralTerm);
            if(bestSet == null){
                bestSet = new ArrayList<>(1);
                bestSet.add(s);
                bestByCentralTerm.put(centralTerm, bestSet);
                return 1;
            }else{
                Set<String> neighborsScore = s.getNeighborEdgesDFsBiggerThanOne(stats);
                boolean presentsNovelty = true;
                for(LinkedElement item : bestSet){
                    Set<String> neighborScore2 = item.getNeighborEdgesDFsBiggerThanOne(stats);
                    if(neighborsScore.isEmpty() && neighborScore2.isEmpty() && s.getNumEdges() < item.getNumEdges()){ //troca item por s devido ter menos arestas sem utilidade
                        bestSet.remove(item);
                        bestSet.add(s);
                        return 2;
                    }
                    if(DataStructureUtils.differenceCount(neighborsScore, neighborScore2) == 0)
                        presentsNovelty = false;
                }
                if(presentsNovelty){
                    bestSet.add(s);
                    return 1;
                }
            }
        }
        return 0;
    }

    public abstract void generate(SubgraphsHandler subgraphsHandler, GraphDatasetStats stats, File codebookSetFile, File codebookFile);

    protected <T extends SampleSubgraph> void export(Stream<T> codebookSet, SubgraphsHandler subgraphsHandler, File outputFile) {
        MutableInt count = new MutableInt();
        try( Writer writer = FileUtils.createWriterToFile(outputFile) ){
            codebookSet.forEach(el -> {
        	    subgraphsHandler.append(el, writer);
                count.increment();
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Logs.info("#codebookSet: "+count);
    }

    private void reduceCodebookSet(File codebookSetFile, int codebookSetSize) {
        try {
			if (maxElementsToUseFromCodebookSet > 0 && codebookSetSize > maxElementsToUseFromCodebookSet) {
			    File newCodebookSetFile = new File(codebookSetFile.getParent(), codebookSetFile.getName() + "_" + maxElementsToUseFromCodebookSet);
			    Logs.finest("Reducing codebookSet up to " + maxElementsToUseFromCodebookSet+" elements (from "+codebookSetSize+")");
			    FileUtils.generateRandomUniqueLineSubset(codebookSetFile, newCodebookSetFile, maxElementsToUseFromCodebookSet);
				if(!codebookSetFile.delete())
					throw new RuntimeException("codebookSetFile could not be reduced due problem for deleting it");
				FileUtils.moveFile(newCodebookSetFile, codebookSetFile);
				Logs.finest("codebookSet reduced successfully");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    public boolean isDatasetStatsRequired() {
        return filterCodebookSet;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
