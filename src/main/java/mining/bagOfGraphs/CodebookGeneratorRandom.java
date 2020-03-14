package mining.bagOfGraphs;

import java.io.File;
import mining.bagOfGraphs.sampleSubgraph.SubgraphsHandler;
import mining.textMining.textToGraph.GraphDatasetStats;
import util.Logs;
import util.io.FileUtils;

public class CodebookGeneratorRandom extends CodebookGenerator {

    private final int maxCodebookSize;

    public CodebookGeneratorRandom(boolean onlyComplexSubgraphsForCodebookSet, int maxElementsBySampleForCodebookSet, boolean filterCodebookSet, int selectionLimit) {
        super(onlyComplexSubgraphsForCodebookSet, maxElementsBySampleForCodebookSet, filterCodebookSet, -1);
    	maxCodebookSize = selectionLimit;
    }

    public int getMaxCodebookSize(){
		return maxCodebookSize;
	}

    @Override
    public void generate(SubgraphsHandler subgraphsHandler, GraphDatasetStats stats, File codebookSetFile, File codebookFile) {
        Logs.fine("Generating codebook using random selection of up to " + maxCodebookSize + " attributes");
        FileUtils.generateRandomUniqueLineSubset(codebookSetFile, codebookFile, maxCodebookSize);
    }
}
