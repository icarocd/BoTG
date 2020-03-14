package mining;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.lang3.mutable.MutableInt;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import util.Collector;
import util.DataStructureUtils;
import util.ListCollector;
import util.Logs;
import util.MathUtils;
import util.Pair;

public abstract class Dataset<T extends Sample> implements Collector<T>, Iterable<T> {

	public static final String ID_COLUMN_NAME = "ID";
	public static final String LABEL_COLUMN_NAME = "LABEL";

    protected ArrayList<T> samples;

    public Dataset() {
        this(new ArrayList<T>());
    }

    public Dataset(ArrayList<T> samples) {
        this.samples = samples;
    }

    @Override
    public void collect(T element) {
        addSample(element);
    }

    public int size() {
        return samples.size();
    }

    @Override
    public Iterator<T> iterator() {
        return samples.iterator();
    }

    public ArrayList<T> getSamples() {
        return samples;
    }

    public T get(int index) {
        return samples.get(index);
    }

    public void addSample(T sample) {
        samples.add(sample);
    }

    public Set<Long> getSampleIds() {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for(T sample : samples){
            ids.add(sample.getId());
        }
        return ids;
    }

    /**
     * returns:
     * - number of samples by class
     */
    public Map<String, MutableInt> getSampleCountByClass() {
        Map<String, MutableInt> sampleCountByClass = new HashMap<>();
        for (T sample : samples) {
            if(sample.getNumberLabels() > 1)
                throw new UnsupportedOperationException("multi-labeled samples are not supported yet");
            DataStructureUtils.incrementMapValue(sampleCountByClass, sample.getLabel());
        }
        return sampleCountByClass;
    }

    /**
     * returns:
     * - pairs of <class,num_samples>
     * - pairs of <id,class>
     * PS: assumes that each sampel is uni-labeled.
     */
    public Pair<Map<String, MutableInt>, SortedMap<Long, String>> loadInfo() {
		//coleta informações sobre a coleção, necessárias na etapa de avaliação:
		Map<String, MutableInt> numSamplesByClass = new TreeMap();
		SortedMap<Long, String> classBySampleId = new TreeMap();
        for(Sample sample : samples){
        	if(sample.getNumberLabels() > 1)
                throw new UnsupportedOperationException("multi-labeled samples are not supported yet");
            String label = sample.getLabel();
			DataStructureUtils.incrementMapValue(numSamplesByClass, label);
            classBySampleId.put(sample.getId(), label);
        }
        return new Pair(numSamplesByClass, classBySampleId);
	}

    public void logSampleCountByClass() {
        Map<String, MutableInt> sampleCountByClass = DataStructureUtils.getMapSortedByValue(getSampleCountByClass(), true);
        Logs.fine("#samples (total "+ MathUtils.sum(sampleCountByClass.values()) +") by classes ("+ sampleCountByClass.size() +"): " + sampleCountByClass);
    }

    /**
     * Scramble the sample positions
     */
    public void randomize(Random random) {
        for (int j = size() - 1; j > 0; j--) {
            swapSamplePositions(j, random.nextInt(j + 1));
        }
    }

    private void swapSamplePositions(int i, int j) {
        T in = samples.get(i);
        samples.set(i, samples.get(j));
        samples.set(j, in);
    }

    /**
     * Stratifies a set of instances according to its class values if the class attribute is nominal (so that afterwards a stratified cross-validation can be
     * performed).
     * @param numFolds the number of folds in the cross-validation
     */
    public void stratify(int numFolds) {
    	if (numFolds <= 1) {
            throw new IllegalArgumentException("Number of folds must be greater than 1");
        }

        // sort by class
        int index = 1;
        int size = size();
        while (index < size) {
            T instance1 = get(index - 1);
            for (int j = index; j < size; j++) {
                T instance2 = get(j);
                if (instance1.getLabel().equals(instance2.getLabel())) {
                    swapSamplePositions(index, j);
                    index++;
                }
            }
            index++;
        }
        stratStep(numFolds);
    }

    /**
     * Help function needed for stratification of set.
     * @param numFolds the number of folds for the stratification
     */
    private void stratStep(int numFolds) {
        ArrayList<T> newVec = new ArrayList<>(size());
        int start = 0, j;

        // create stratified batch
        while (newVec.size() < size()) {
            j = start;
            while (j < size()) {
                newVec.add(get(j));
                j = j + numFolds;
            }
            start++;
        }
        samples = newVec;
    }

    /**
     * Creates the training set for one fold of a cross-validation on the dataset.
     * Requires: 2 <= numFolds && numFolds < size()
     * Requires: 0 <= numFold && numFold < numFolds
     * @param numFolds the number of folds in the cross-validation. Must be greater than 1.
     * @param numFold 0 for the first fold, 1 for the second, ...
     * @return the training set
     * @throws IllegalArgumentException if the number of folds is less than 2 or greater than the number of instances.
     */
    //
    public ArrayList<T> trainCV(int numFolds, int numFold) {
        if (numFolds < 2) {
            throw new IllegalArgumentException("Number of folds must be at least 2!");
        }
        int size = size();
        if (numFolds > size) {
            throw new IllegalArgumentException("Can't have more folds than instances!");
        }
        int numInstForFold = size / numFolds;
        int offset;
        if (numFold < size % numFolds) {
            numInstForFold++;
            offset = numFold;
        } else {
            offset = size % numFolds;
        }
        ArrayList<T> train = new ArrayList<>(size - numInstForFold);
        int first = numFold * (size / numFolds) + offset;
        copySamples(0, train, first);
        copySamples(first + numInstForFold, train, size() - first - numInstForFold);
        return train;
    }

    /**
     * Creates the test set for one fold of a cross-validation on the dataset.
     *
     * Requires: 2 <= numFolds && numFolds < size()
     * Requires: 0 <= numFold && numFold < numFolds
     *
     * @param numFolds the number of folds in the cross-validation. Must be greater than 1.
     * @param numFold 0 for the first fold, 1 for the second, ...
     * @return the test set as a set of weighted instances
     * @throws IllegalArgumentException if the number of folds is less than 2 or greater than the number of instances.
     */
    public ArrayList<T> testCV(int numFolds, int numFold) {
        if (numFolds < 2) {
            throw new IllegalArgumentException("Number of folds must be at least 2!");
        }
        int size = size();
        if (numFolds > size) {
            throw new IllegalArgumentException("Can't have more folds than instances!");
        }
        int first, offset;
        int numInstForFold = size / numFolds;
        if (numFold < size % numFolds) {
            numInstForFold++;
            offset = numFold;
        } else {
            offset = size % numFolds;
        }
        ArrayList<T> test = new ArrayList<>(numInstForFold);
        first = numFold * (size / numFolds) + offset;
        copySamples(first, test, numInstForFold);
        return test;
    }

    /**
     * Copies instances from one set to the end of another one.
     *
     * Requires: 0 <= from && from <= size() - num
     * Requires: 0 <= num
     *
     * @param from the position of the first instance to be copied
     * @param dest the destination for the instances
     * @param num the number of instances to be copied
     */
    private void copySamples(int from, List<T> dest, int num) {
        for (int i = 0; i < num; i++) {
            dest.add(get(from + i));
        }
    }

    /** Given a fold descriptor file, returns the files from such dataset, pairs of <sample_file_name,sample_label> */
    public static List<Pair<String,String>> loadSubsetDescritor(File subsetDescriptorFile){
        ListCollector<Pair<String, String>> collector = new ListCollector<>();
        loadSubsetDescritor(subsetDescriptorFile, collector);
        return collector.getElements();
    }

    /** Given a fold descriptor file, collects the files from such dataset, as pairs of <sample_file_name,sample_label> */
    public static void loadSubsetDescritor(File subsetDescriptorFile, Collector<Pair<String,String>> collector){
        //  <dataset>
        //    <sample class="earn" id="5593"/>
        //  </dataset>
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(subsetDescriptorFile, new DefaultHandler(){
                public void startElement(String uri, String localName, String tagName, Attributes attributes) {
                    if(tagName.equalsIgnoreCase("sample")){
                        collector.collect(new Pair<>(attributes.getValue("id"), attributes.getValue("class")));
                    }
                }
            });
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
	public static Iterable<File> loadSubsetFiles(File containerDir, boolean skipMissingFiles, List<Pair<String,String>>... subsets) {
        List<File> files = new ArrayList<>();
		for (List<Pair<String,String>> subset : subsets) {
			for (Pair<String, String> sample_filenameAndLabel : subset) {
				File f = new File(containerDir, sample_filenameAndLabel.getA());
                if (!skipMissingFiles || f.exists()) {
                    files.add(f);
                }
			}
		}
        return files;
    }

    public LinkedHashSet<String> getLabels() {
        return getLabels(samples);
    }
    public SortedSet<String> getLabelsInOrder() {
        return getLabelsInOrder(samples);
    }
    public static LinkedHashSet<String> getLabels(Iterable<? extends Sample> samples) {
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (Sample s : samples)
            labels.add(s.getLabel());
        return labels;
    }
    public static SortedSet<String> getLabelsInOrder(Iterable<? extends Sample> samples) {
        TreeSet<String> labels = new TreeSet<>();
        for (Sample s : samples)
            labels.add(s.getLabel());
        return labels;
    }

	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(!(obj instanceof Dataset))
			return false;
		Dataset other = (Dataset) obj;
		if (samples == null) {
			if(other.samples != null)
				return false;
		} else if (!samples.equals(other.samples)) {
			return false;
		}
		return true;
	}
}
