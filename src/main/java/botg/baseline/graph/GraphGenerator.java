package botg.baseline.graph;

import java.io.File;
import java.util.logging.Level;
import mining.DatasetCrossFold;
import mining.DatasetReader;
import mining.SamplePathResolverSimple;
import mining.textMining.TextSample;
import mining.textMining.parsing.TokenExtractor;
import mining.textMining.textToGraph.GraphDataset;
import mining.textMining.textToGraph.TextSampleToGraphSampleConverter;
import mining.textMining.textToGraph.model.GraphType;
import util.Filter;
import util.Logs;
import util.Params;
import util.io.FileUtils;

public class GraphGenerator {

    public static void main(String[] args) {
        GraphGeneratorConfigs configs = new GraphGeneratorConfigs(Params.parse(args));
        FileUtils.mkDirs(configs.getTextAsGraphRootFolder());

        Logs.init(Level.FINER, new File(configs.getTextAsGraphRootFolder(), GraphGenerator.class.getSimpleName() + ".log"));

        Logs.info("Common configs used:\n" + configs);

		for (GraphType graphType : configs.graphTypes) {
            Logs.info("# Running for graphType " + graphType + "...");

            File destineFolder = configs.getDatasetRepresentationDestineFolder(graphType);
            FileUtils.cleanOtherwiseCreateDirectory(destineFolder);

			execute(configs.createDatasetReader(), configs.createTokenExtractor(),
            	configs.getFoldDistributionToUseInReduction(), configs.textSampleFilter,
            	graphType, configs.FORCE_REACH, configs.MAX_REACH,
            	configs.minimumTermDF, configs.induceEdgesAfterTermPrunning, configs.useTfIdf,
            	configs.maxNodesByGraphToRetain, destineFolder);
        }
    }

    private static void execute(DatasetReader<TextSample> datasetReader, TokenExtractor tokenExtractor,
        DatasetCrossFold datasetCrossFold, Filter<TextSample> textSampleFilter,
        GraphType graphType, boolean forceReach, int maxNeighborhoodReach,
        int minimumTermDF, boolean induceEdgesAfterTermPrunning, boolean useTfIdf, int maxNodesByGraphToRetain, File destineFolder)
    {
        if (datasetCrossFold != null) {
            if (textSampleFilter != null) {
                Logs.warn("Due DatasetCrossFold was set, these options will be ignored: sampleFilter");
                textSampleFilter = null;
            }
            //se datasetCrossFold setado, aceitar somente as amostras contidas na distribuição de folds:
            textSampleFilter = sample -> datasetCrossFold.containsSample(sample.getId());
        }

        GraphDataset dataset = new GraphDataset();

        TextSampleToGraphSampleConverter textSampleCollector = new TextSampleToGraphSampleConverter(
            tokenExtractor, graphType, forceReach, maxNeighborhoodReach, dataset);

        datasetReader.readSamples(textSampleFilter, textSampleCollector);

        if (datasetCrossFold != null) {
            datasetCrossFold.apply(dataset);
        }

        dataset.pruneTermsFromSamplesByDF(minimumTermDF, induceEdgesAfterTermPrunning);

        if (graphType.isCountingRequired()) {
            if (useTfIdf) {
                dataset.convertTFtoTFIDF();
                dataset.normalizeWeights(0,1);
            } else if (graphType.isCountingRequiredAsRelative()) {
                dataset.normalizeWeights(0,1);
            }
        }

        dataset.logStats();

        if (maxNodesByGraphToRetain > 0) {
            dataset.pruneWorstWeightedTerms(maxNodesByGraphToRetain);
            dataset.logStats();
        }

        dataset.writeToFolder(new SamplePathResolverSimple(destineFolder));
    }
}
