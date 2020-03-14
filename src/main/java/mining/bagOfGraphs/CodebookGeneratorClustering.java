package mining.bagOfGraphs;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import mining.bagOfGraphs.sampleSubgraph.SampleSubgraph;
import mining.bagOfGraphs.sampleSubgraph.SubgraphsHandler;
import mining.clustering.Clusterer;
import mining.textMining.textToGraph.GraphDatasetStats;
import util.Factory;
import util.Logs;
import util.TimeWatcher;
import util.dataStructure.Matrix;
import util.io.FileUtils;

public class CodebookGeneratorClustering extends CodebookGenerator {

	private final boolean prioritizeMemoryInsteadOfSpeed;
    private final Factory<Clusterer> clusterer;

    /**
     * @param maxElementToUseFromCodebookSet -1 to use all codebookSet on codebook generation.
     *  Avoid high numbers, due requirement of lots of space on disk. Ex: for 50,000, matrix will require 50,000 * 50,000 * 4 bytes (~ 10GB)
     *  the algorithm will use disk. Ex: for 10,000, the matrix would require 10,000 * 10,000 * 4 bytes in memory (~382 MB); for 13,000: ~645MB.
     */
    public CodebookGeneratorClustering(boolean onlyComplexSubgraphsForCodebookSet, int maxElementsBySampleForCodebookSet,
        boolean filterCodebookSet, int maxElementToUseFromCodebookSet, boolean prioritizeMemoryInsteadOfSpeed, Factory<Clusterer> clusterer)
    {
    	super(onlyComplexSubgraphsForCodebookSet, maxElementsBySampleForCodebookSet, filterCodebookSet, maxElementToUseFromCodebookSet);
    	this.prioritizeMemoryInsteadOfSpeed = prioritizeMemoryInsteadOfSpeed;
        this.clusterer = clusterer;
    }

    @Override
    public void generate(SubgraphsHandler subgraphsHandler, GraphDatasetStats stats, File codebookSetFile, File codebookFile) {
        //computa as distancias entre os elementos do codebookSet:
        Matrix<Float> codebookSetDistanceMatrix = computeCodebookSetDistanceMatrix(codebookSetFile, subgraphsHandler);

        //gera o codebook:
        generate(codebookSetDistanceMatrix, codebookSetFile, codebookFile);

        codebookSetDistanceMatrix.destroyResources();
    }

    private Matrix<Float> computeCodebookSetDistanceMatrix(File codebookSetFile, SubgraphsHandler subgraphsHandler) {
        TimeWatcher watcher = new TimeWatcher();

        final List<SampleSubgraph> codebookCandidates = subgraphsHandler.loadSamplesSubgraphs(codebookSetFile, maxElementsToUseFromCodebookSet);

        Logs.finest("Computing codebookSet distance matrix, for "+codebookCandidates.size()+" subgraphs...");

        Matrix<Float> D = subgraphsHandler.computeDistanceMatrix(codebookCandidates, prioritizeMemoryInsteadOfSpeed);

        Logs.finer("CodebookSet distance matrix computed in " + watcher);

        return D;
    }

    private void generate(Matrix<Float> codebookSetDistanceMatrix, File codebookSetFile, File codebookOutputFile) {
        TimeWatcher watcher = new TimeWatcher();
    	List<Integer> clustersIndices = clusterer.create(codebookSetDistanceMatrix).cluster();
        Logs.finer("Clustering of codebookSet finished after " + watcher);
        Logs.info("#codebook: " + clustersIndices.size());
        save(clustersIndices, codebookSetFile, codebookOutputFile);
    }

    private void save(List<Integer> centersIndices, File codebookSetFile, File codebookOutputFile) {
        try( Writer fileWriter = FileUtils.createWriterToFile(codebookOutputFile) ){
            List<String> lines = FileUtils.readLines(codebookSetFile);
            for(int c : centersIndices)
                fileWriter.append(lines.get(c)).append('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
