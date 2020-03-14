package mining.textMining.bagOfWord;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import mining.Dataset;
import mining.textMining.WeightMeasurer;
import util.DataStructureUtils;
import util.Logs;
import util.io.FileUtils;
import util.io.MatrixWriter;

public class BagOfWordDataset extends Dataset<BagOfWordSample> {

    //cache local dos DF's dos termos do dataset, criado lazy sob demanda, e mantido para evitar recomputo em usos subsequentes
    private Map<String, MutableInt> termsDFs;

    public BagOfWordDataset() {
    }

    public BagOfWordDataset(ArrayList<BagOfWordSample> samples) {
        super(samples);
    }

    public void discardCachedData() {
        termsDFs = null;
    }

    private SortedSet<String> getTermsInOrder() {
        TreeSet<String> terms = new TreeSet<>();
        for (BagOfWordSample sample : samples) {
            for(String term : sample.getElementsWeights().keySet()){
                terms.add(term);
            }
        }
        return terms;
    }

    private Map<String, MutableInt> getDocumentFrequencyFromTerms() {
        if(termsDFs == null){
            termsDFs = new HashMap<>();
            for (BagOfWordSample bagOfWordSample : samples) {
                Set<String> sampleTerms = bagOfWordSample.getElementsWeights().keySet();
                for(String term : sampleTerms){
                    DataStructureUtils.incrementMapValue(termsDFs, term);
                }
            }
        }
        return termsDFs;
    }

    public void removeUnitaryTermsFromSamples() {
        Map<String, MutableInt> termsDFs = getDocumentFrequencyFromTerms();

        Set<String> unitaryTerms = new LinkedHashSet<>();
        for (Iterator<Entry<String, MutableInt>> it = termsDFs.entrySet().iterator(); it.hasNext(); ) {
            Entry<String, MutableInt> termDF = it.next();

            if(termDF.getValue().intValue() == 1){
                unitaryTerms.add(termDF.getKey());
                it.remove();
            }
        }

        for (BagOfWordSample sample : samples) {
            Map<String, MutableDouble> elementsWeights = sample.getElementsWeights();

            //System.out.println("Number of original terms from sample " + sample.getId() + ": " + elementsWeights.size());
            for (String unitaryTerm : unitaryTerms) {
                elementsWeights.remove(unitaryTerm);
            }
            //System.out.println("Number of final terms from sample " + sample.getId() + ": " + elementsWeights.size());
        }
    }

    public void convertTFtoTFIDF() {
        Map<String, MutableInt> termsDFs = getDocumentFrequencyFromTerms();
        convertTFtoTFIDF(termsDFs);
    }
    public void convertTFtoTFIDF(Map<String, MutableInt> termsDFs) {
        int nDocuments = samples.size();
        for (BagOfWordSample sample : samples) {
    		WeightMeasurer.convertTFToTFIDF(nDocuments, termsDFs, sample.getElementsWeights());
        }
    }

    public void writeToFile(File resultFile, boolean sparse) {
        List<String> sortedTerms = new ArrayList<>(getTermsInOrder());
        writeToFile(resultFile, sparse, sortedTerms);
    }
    public void writeToFile(File resultFile, boolean sparse, List<String> sortedTerms) {
        final int dimensions = sortedTerms.size();

        logSampleCountByClass();
        Logs.info("terms ("+dimensions+"): " + sortedTerms);

        FileUtils.deleteQuietly(resultFile);
        FileUtils.mkDirsForFile(resultFile);

        try( MatrixWriter writer = new MatrixWriter(resultFile) ){
            //header:
            writer.add(ID_COLUMN_NAME);
            writer.separateAndAdd(LABEL_COLUMN_NAME);
            for(String term : sortedTerms)
                writer.separateAndAdd(term);

            for (BagOfWordSample sample : samples) {
                writer.newLine();
                writer.add(sample.getId());

                int numLabels = sample.getNumberLabels();
                if(numLabels > 1)
                    throw new UnsupportedOperationException("multi-labeled samples are not supported yet");
                writer.separateAndAdd(numLabels == 1 ? sample.getFirstLabel() : "");

                if(sparse){
                    for (int j = 0; j < dimensions; j++) {
                        String term = sortedTerms.get(j);
                        double weight = sample.getElementWeight(term);
                        if(weight != 0.0)
                            writer.separateAndAdd(j+"=").addFormattedDecimal(weight);
                    }
                }else{
                    for (int j = 0; j < dimensions; j++) {
                        String term = sortedTerms.get(j);
                        double weight = sample.getElementWeight(term);
                        writer.separateAndAddFormattedDecimal(weight);
                    }
                }
            }
        }
    }
}
