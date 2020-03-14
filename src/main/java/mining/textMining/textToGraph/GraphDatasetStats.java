package mining.textMining.textToGraph;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.mutable.MutableInt;
import util.DataStructureUtils;
import util.Pair;

public class GraphDatasetStats {

    private Map<String, MutableInt> termsDFs;
    private Map<Pair<String, String>, MutableInt> pairTermDFs;

    public GraphDatasetStats() {
        this.termsDFs = new HashMap<>();
        this.pairTermDFs = new HashMap<>();
    }

    public void incrementTermDF(String term) {
        DataStructureUtils.incrementMapValue(termsDFs, term);
    }

    public void incrementPairTermDF(Pair<String, String> sourceTarget) {
        DataStructureUtils.incrementMapValue(pairTermDFs, sourceTarget);
    }

    public Map<String, MutableInt> getTermsDFs() {
        return termsDFs;
    }

    public int getTermDF(String term) {
        return termsDFs.get(term).intValue();
    }

    public int getEdgeDF(String termA, String termB) {
        return pairTermDFs.get(new Pair<>(termA, termB)).intValue();
    }
}
