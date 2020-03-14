package mining.textMining.bagOfWord;

import java.io.File;
import mining.DatasetCrossFold;
import mining.DatasetReader;
import mining.textMining.TextSample;
import mining.textMining.bagOfWord.BagOfWordDataset;
import mining.textMining.bagOfWord.TextSampleToBagOfWordSampleConverter;
import mining.textMining.gram.TextSampleToGramConverter;
import mining.textMining.parsing.TokenExtractor;
import util.Collector;
import util.Filter;
import util.Logs;

public class DatasetToBagOfWordsReducer {

    public void execute(DatasetReader<TextSample> datasetReader, TokenExtractor tokenExtractor, int gramSize,
    	DatasetCrossFold datasetCrossFold, Filter<TextSample> textSampleFilter,
    	boolean discardUnitaryTerms, boolean useTfIdf, File destineFile) {

		if (datasetCrossFold != null) {
			if (textSampleFilter != null) {
				Logs.warn("Due DatasetCrossFold was set, these options will be ignored: sampleFilter");
				textSampleFilter = null;
			}
			//se datasetCrossFold setado, aceitar somente as amostras contidas na distribuição de folds:
			textSampleFilter = sample -> datasetCrossFold.containsSample(sample.getId());
    	}

        BagOfWordDataset dataset = new BagOfWordDataset();

        Collector<TextSample> textSampleCollector = gramSize == 1 ? new TextSampleToBagOfWordSampleConverter(tokenExtractor, null, dataset) : new TextSampleToGramConverter(tokenExtractor, null, gramSize, dataset);
        datasetReader.readSamples(textSampleFilter, textSampleCollector);

		if (datasetCrossFold != null) {
        	datasetCrossFold.apply(dataset);
        }

        if (discardUnitaryTerms) {
            dataset.removeUnitaryTermsFromSamples();
        }

        if (useTfIdf) {
            dataset.convertTFtoTFIDF();
        }

        dataset.discardCachedData();
        dataset.writeToFile(destineFile, true);
    }
}
