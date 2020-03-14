package mining.textMining.bagOfWord;

import java.util.Set;
import java.util.function.BiConsumer;
import mining.Sample;
import util.DataStructureUtils;
import util.FloatVector;

public class VectorSample extends Sample {

    private final FloatVector weights;

    public VectorSample(long id, String label, FloatVector weights) {
        this(id, label==null ? null : DataStructureUtils.asSet(label), weights);
    }
    public VectorSample(long id, Set<String> labels, FloatVector weights) {
        super(id, labels);
        this.weights = weights;
    }

    public int getNumDimensions() {
		return weights.length();
	}

    public FloatVector weights() {
        return weights;
    }

    public void set(int index, float weight) {
		weights.set(index, weight);
	}

    public float get(int attributeIdx) {
        return weights.get(attributeIdx);
    }

    /**
     * Provides an abstract way to iterate over each <index,value> containing non-zero values within the vector
     */
    public void forEachNonZero(BiConsumer<Integer,Float> task) {
        weights.forEachNonZero(task);
    }

    public int getNumNonZeroValues() {
        return weights.getNumNonZeroValues();
    }
}
