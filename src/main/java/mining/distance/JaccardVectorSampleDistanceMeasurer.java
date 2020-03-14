package mining.distance;

import mining.textMining.bagOfWord.VectorSample;
import util.MathUtils;

public class JaccardVectorSampleDistanceMeasurer extends SampleDistanceMeasurer<VectorSample> {

    @Override
    public float getDistance(VectorSample sample1, VectorSample sample2) {
        return MathUtils.jaccardDistance(sample1.weights(), sample2.weights());
    }
    @Override
    public float getSimilarity(VectorSample sample1, VectorSample sample2) {
    	return MathUtils.jaccardSimilarity(sample1.weights(), sample2.weights());
    }
}
