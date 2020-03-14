package mining.textMining;

import mining.SampleFilter;

public class TextSampleFilter extends SampleFilter<TextSample> {

    private boolean skipEmptySamples;

    public TextSampleFilter(boolean skipUnlabeledSamples, boolean skipMultiLabeledSamples, boolean skipEmptySamples) {
        super(skipUnlabeledSamples, skipMultiLabeledSamples);
        this.skipEmptySamples = skipEmptySamples;
    }

    @Override
    public boolean isAccepted(TextSample sample) {
        if(!super.isAccepted(sample)){
            return false;
        }
        if (skipEmptySamples && sample.isBodyEmpty()) {
            return false;
        }
        return true;
    }
}
