package mining.textMining.bagOfWord;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.mutable.MutableDouble;
import mining.Sample;
import util.DataStructureUtils;

public class BagOfWordSample extends Sample {

    Map<String,MutableDouble> elementsWeights;

    public BagOfWordSample(long id, Set<String> labels) {
        super(id, labels);
        elementsWeights = new LinkedHashMap<>();
    }

    public Map<String, MutableDouble> getElementsWeights() {
        return elementsWeights;
    }

    public void incrementWeightToElement(String token) {
        DataStructureUtils.incrementMapValueDouble(elementsWeights, token);
    }

    public double getElementWeight(String term) {
        MutableDouble weigth = elementsWeights.get(term);
        return weigth != null ? weigth.doubleValue() : 0;
    }
}
