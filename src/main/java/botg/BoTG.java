package botg;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import botg.config.BoTGConfigs;
import mining.Dataset;
import mining.DatasetCrossFold;
import mining.SamplePathResolver;
import mining.bagOfGraphs.BoGCreator;
import mining.bagOfGraphs.CodebookGenerator;
import mining.bagOfGraphs.TypeAssignment;
import mining.bagOfGraphs.TypePooling;
import mining.bagOfGraphs.sampleSubgraph.SampleSubgraph;
import mining.bagOfGraphs.sampleSubgraph.SubgraphsHandler;
import mining.textMining.bagOfWord.VectorDataset;
import mining.textMining.bagOfWord.VectorSample;
import mining.textMining.textToGraph.GraphDataset;
import mining.textMining.textToGraph.GraphDatasetStats;
import util.DateUtil;
import util.Logs;
import util.Pair;
import util.Params;
import util.StringUtils;

public class BoTG {

    public static void main(String[] args) {
    	try {
            Params params = Params.parse(args);
            BoTGConfigs configs = new BoTGConfigs(params);
            String outputDirname = params.get("outputDirname");
            args = null; params = null;
            run(outputDirname, configs);
        } catch (Throwable t) {
            Logs.severe(t);
        }
    }

	private static void run(String outputDirname, BoTGConfigs configs) {
	    File outputDir;
	    if(StringUtils.isNotEmpty(outputDirname)){
	        outputDir = new File(configs.getDatasetResultsFolder(), outputDirname);
	    }else{
	        outputDir = configs.createNewOutputFolder();
	    }
		Logs.init(Level.FINEST, new File(outputDir, BoTG.class.getSimpleName() + "_" + DateUtil.formatDateTimeFull() + ".txt"));
	    Logs.info("Running for configs: " + configs);

        run(configs.getGraphsFolder(), configs.getFoldDistributionsDir(), configs.subgraphsHandler,
        	configs.codebookGenerator, configs.assignment, configs.pooling,
            configs.nFolds, outputDir);
	}

    private static void run(SamplePathResolver samplesFolder, File foldDistributionsDir, SubgraphsHandler subgraphsHandler,
        CodebookGenerator codebookGenerator, TypeAssignment assignmentType, TypePooling poolingType, int numFolds, File outputDir)
    {
        final boolean skipMissingSamples = true; //ignoramos arquivos faltantes pois certos filtros do graph generator podem skipar amostras (ex: prune de termos torna amostras vazias e as ignoramos)

        File subgraphsDir = new File(outputDir, "subgraphs");
        subgraphsHandler.extractAndSaveSampleSubgraphs(samplesFolder, subgraphsDir, false);

    	GraphDatasetStats datasetStats = null;
    	if(codebookGenerator.isDatasetStatsRequired())
    	    datasetStats = GraphDataset.loadFromFolder(samplesFolder).computeStatistics();

        for (int foldNumber = 0; foldNumber < numFolds; foldNumber++) {
			Logs.fine("Running for fold=" + foldNumber);
			List<Pair<String,String>> trainSubset = GraphDataset.loadSubsetDescritor(DatasetCrossFold.getTrainFoldFile(foldDistributionsDir, foldNumber));
			List<Pair<String,String>> testSubset = GraphDataset.loadSubsetDescritor(DatasetCrossFold.getTestFoldFile(foldDistributionsDir, foldNumber));

    		File codebookFile = new File(outputDir, foldNumber + "_codebook");
    		codebookGenerator.generate(subgraphsHandler, datasetStats, Dataset.loadSubsetFiles(subgraphsDir, skipMissingSamples, trainSubset), codebookFile);
    		List<SampleSubgraph> codebook = subgraphsHandler.loadSamplesSubgraphs(codebookFile, -1);

    	    ArrayList<VectorSample> trainBoGs, testBoGs;
	        Pair<ArrayList<VectorSample>,ArrayList<VectorSample>> bogs = obtainBoGs(subgraphsHandler, subgraphsDir, skipMissingSamples, codebook, assignmentType, poolingType,
                trainSubset, testSubset);
	        trainBoGs = bogs.getA();
	        testBoGs = bogs.getB();
		    VectorDataset.normalizeAttributes(trainBoGs, testBoGs);
		    new VectorDataset(trainBoGs).writeToFile(new File(outputDir,foldNumber+"_trainVectors"), true, true);
		    new VectorDataset(testBoGs).writeToFile(new File(outputDir,foldNumber+"_testVectors"), true, true);
        }
    }

    private static Pair<ArrayList<VectorSample>, ArrayList<VectorSample>> obtainBoGs(SubgraphsHandler subgraphsHandler, File subgraphsDir, boolean skipMissingSamples,
        List<SampleSubgraph> codebook, TypeAssignment assignmentType, TypePooling poolingType, List<Pair<String, String>> trainSubset, List<Pair<String, String>> testSubset)
    {
    	ArrayList<VectorSample> trainBoGs = new ArrayList<>(), testBoGs = new ArrayList<>();
        BoGCreator.createBoGs(subgraphsHandler, subgraphsDir, skipMissingSamples, codebook, assignmentType, poolingType, v -> trainBoGs.add(v), trainSubset);
        BoGCreator.createBoGs(subgraphsHandler, subgraphsDir, skipMissingSamples, codebook, assignmentType, poolingType, v -> testBoGs.add(v), testSubset);
        Collections.sort(trainBoGs, VectorSample.COMPARATOR_BY_ID);
        Collections.sort(testBoGs, VectorSample.COMPARATOR_BY_ID);
        return new Pair<>(trainBoGs, testBoGs);
    }
}
