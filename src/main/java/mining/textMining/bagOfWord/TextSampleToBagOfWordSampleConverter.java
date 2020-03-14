package mining.textMining.bagOfWord;

import java.util.function.Predicate;
import mining.textMining.TextSample;
import mining.textMining.parsing.TokenCollector;
import mining.textMining.parsing.TokenExtractor;
import util.Collector;
import util.Logs;

/**
 * {@link Collector} of {@link TextSample} that, for each received {@link TextSample}, produces a {@link BagOfWordSample} and
 * handles it to the supplied {@link Collector} of {@link BagOfWordSample}.
 */
public class TextSampleToBagOfWordSampleConverter implements Collector<TextSample> {

    private TokenExtractor tokenExtractor;
    private Predicate<String> acceptedTerms;
    private Collector<BagOfWordSample> sampleCollector;

    public TextSampleToBagOfWordSampleConverter(TokenExtractor tokenExtractor, Predicate<String> acceptedTerms, Collector<BagOfWordSample> sampleCollector) {
        this.tokenExtractor = tokenExtractor;
        this.acceptedTerms = acceptedTerms;
        this.sampleCollector = sampleCollector;
    }

    @Override
    public void collect(TextSample sample) {
        BagOfWordSample bowSample = new BagOfWordSample(sample.getId(), sample.getLabels());
        TokenCollector tokenCollector = new TokenCollector() {
            @Override
            public void collect(String token) {
                if(acceptedTerms==null || acceptedTerms.test(token))
                    bowSample.incrementWeightToElement(token);
            }
            @Override
            public void subSectionStarted() {}
        };

        tokenExtractor.extract(sample.getTitle(), tokenCollector);
        tokenExtractor.extract(sample.getBody(), tokenCollector);

        if(!bowSample.getElementsWeights().isEmpty()){
        	sampleCollector.collect(bowSample);
        }else{
        	Logs.finer("Sample discarded due empty terms: " + bowSample.getId());
        }
    }
}
