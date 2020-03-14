package mining.textMining;

import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import util.MathUtils;

public class WeightMeasurer {

    public static double tfIdf(int termFrequency, int maxTermFrequencyFromSample, int documentFrequency, int nDocuments) {
        //ajuste abaixo do TF é pra evitar que textos longos tragam TF's maiores que termos de textos menores
        //valor vai variar no intervalo [0.5, 1]
        double adjustedTermFrequency = 0.5 + ( (0.5 * termFrequency) / maxTermFrequencyFromSample );

        double idf = Math.log(nDocuments / (double)documentFrequency);
        //PS: em casos onde se tem documentFrequency = 0 porque o termo nao consta no corpus, deveriamos trocar o documentFrequency na forma anterior de IDF por (1+documentFrequency)

        return adjustedTermFrequency * idf;

        //PS: Mathematically the base of the log function does not matter and constitutes a constant multiplicative factor towards the overall result
    }

	public static void convertTFToTFIDF(int nDocuments, Map<String, MutableInt> termsDFs, Map<String, MutableDouble> elementsWeights) {
		final int maxTermFrequency = (int) getMaxTermWeight(elementsWeights);
        for (Entry<String, MutableDouble> termWeight : elementsWeights.entrySet()) {
            String term = termWeight.getKey();
            MutableDouble weight = termWeight.getValue();

            int termFrequency = weight.intValue();
            int documentFrequency = termsDFs.get(term).intValue();
            double tfIdf = tfIdf(termFrequency, maxTermFrequency, documentFrequency, nDocuments);

            weight.setValue(tfIdf);
        }
	}

	private static double getMaxTermWeight(Map<String, MutableDouble> elementsWeights) {
    	return MathUtils.max(elementsWeights.values()).doubleValue();
    }
}
