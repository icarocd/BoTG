package mining.textMining.gram;

import java.util.List;
import java.util.function.Predicate;
import com.google.common.base.Preconditions;
import mining.textMining.TextSample;
import mining.textMining.bagOfWord.BagOfWordSample;
import mining.textMining.bagOfWord.TextSampleToBagOfWordSampleConverter;
import mining.textMining.parsing.TextSection;
import mining.textMining.parsing.TokenExtractor;
import mining.textMining.parsing.TokenList;
import util.Collector;
import util.DataStructureUtils;
import util.Logs;

/**
 * {@link Collector} of {@link TextSample} that, for each received {@link TextSample}, produces a {@link BagOfWordSample}
 * considering the specified n-gram size, and handles it to the supplied {@link Collector} of {@link BagOfWordSample}.
 */
public class TextSampleToGramConverter implements Collector<TextSample> {

    private static final String TOKEN_JOINER = "§";

    private final TokenExtractor tokenExtractor;
    private Predicate<String> filteredTerms;
    private final int maxGramSize;
    private final Collector<BagOfWordSample> sampleCollector;

    public TextSampleToGramConverter(TokenExtractor tokenExtractor, Predicate<String> filteredTerms, int maxGramSize, Collector<BagOfWordSample> sampleCollector)
    {
        Preconditions.checkArgument(maxGramSize >= 2, "maxGramSize must be >= 2. For size 1, use " + TextSampleToBagOfWordSampleConverter.class.getSimpleName());
        this.tokenExtractor = tokenExtractor;
        this.filteredTerms = filteredTerms;
        this.maxGramSize = maxGramSize;
        this.sampleCollector = sampleCollector;
    }

    @Override
    public void collect(TextSample sample) {
        List<TextSection> sections = TextSection.loadSections(tokenExtractor, sample);

        BagOfWordSample bowSample = new BagOfWordSample(sample.getId(), sample.getLabels());

        for (TextSection section : sections) {
            for (TokenList subSection : section.getSubSections()) {
                List<String> terms = subSection.getTokens();
                if(filteredTerms != null) {
                    DataStructureUtils.removeAll(terms, t -> !filteredTerms.test(t));
                }

                int nTerms = terms.size();
                for (int termIdx = 0; termIdx < nTerms; termIdx++) {
                    String gram = terms.get(termIdx);
                    bowSample.incrementWeightToElement(gram);
                    int maxIdx = Math.min(termIdx + maxGramSize - 1, nTerms - 1);
                    for (int idx = termIdx + 1; idx <= maxIdx; idx++) {
                        gram += TOKEN_JOINER + terms.get(idx);
                        bowSample.incrementWeightToElement(gram);
                    }
                }
            }
        }

        if(!bowSample.getElementsWeights().isEmpty()){
            sampleCollector.collect(bowSample);
        }else{
            Logs.finer("Sample discarded due empty terms: " + bowSample.getId());
        }
    }
}
