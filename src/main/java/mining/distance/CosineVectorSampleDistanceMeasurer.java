package mining.distance;

import mining.textMining.bagOfWord.VectorSample;
import util.MathUtils;

public class CosineVectorSampleDistanceMeasurer extends SampleDistanceMeasurer<VectorSample> {

    @Override
    public float getDistance(VectorSample sample1, VectorSample sample2) {
        return MathUtils.cosineDistance(sample1.weights(), sample2.weights());
    }

    @Override
    public float getSimilarity(VectorSample sample1, VectorSample sample2) {
        return MathUtils.cosineSimilarity(sample1.weights(), sample2.weights());
    }
}
