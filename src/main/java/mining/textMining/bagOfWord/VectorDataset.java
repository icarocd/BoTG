package mining.textMining.bagOfWord;

import java.io.File;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.commons.lang3.mutable.MutableInt;
import com.google.common.base.Preconditions;
import mining.Dataset;
import mining.Sample;
import mining.SamplePathResolver;
import mining.SamplePathResolverSimple;
import util.Collector;
import util.DataStructureUtils;
import util.FloatVector;
import util.FloatVectorFactory;
import util.ListCollector;
import util.Logs;
import util.Pair;
import util.Pointer;
import util.StringUtils;
import util.TimeWatcher;
import util.dataStructure.IndexableSet;
import util.io.FileUtils;
import util.io.MatrixWriter;
import util.io.ObjectIO;

public class VectorDataset extends Dataset<VectorSample> {

	public VectorDataset() {
    }

    public VectorDataset(ArrayList<VectorSample> samples) {
        super(samples);
    }

    public void normalizeAttributes() {
        normalizeAttributes(samples);
    }
	public static void normalizeAttributes(List<VectorSample>... datasets) {
	    Logs.finest("Normalizing attributes for the " + datasets.length + " sets");
	    int nAttributes = datasets[0].get(0).getNumDimensions();

	    //obtain the min and max for each attribute:
	    float[] minValues = new float[nAttributes];
	    float[] maxValues = new float[nAttributes];
	    //IMPORTANTE: presumimos sempre que o mínimo é zero (vetor min iniciado de zeros) pois este é o valor default nao presente em vetores esparsos...
	    for(Collection<VectorSample> dataset : datasets){
	        for(VectorSample v : dataset){
	            v.forEachNonZero((idx, weight) -> {
	                float min = minValues[idx], max = maxValues[idx];
                    if(weight < min)
	                    minValues[idx] = weight;
	                if(weight > max)
	                    maxValues[idx] = weight;
	            });
	        }
	    }

	    for(Collection<VectorSample> dataset : datasets){
	        for(VectorSample v : dataset){
	            v.forEachNonZero((idx, weight) -> {
	                float min = minValues[idx];
	                float range = maxValues[idx] - min;
	                if(range != 0){
	                    float newWeight = (weight - min) / range;
                        v.set(idx, newWeight);
	                    //System.out.println("id "+v.getId()+ ", idx "+idx+", old " + weight + ", new "+newWeight+", get "+v.get(idx));
	                }
	            });
	        }
	    }
	}
	//implementação anterior, mais lenta pois nao tirava proveito da esparsidão dos dados
	/*@SafeVarargs
    public static void normalizeAttributes(List<VectorSample>... datasets) {
        Logs.finest("Normalizing attributes for the " + datasets.length + " sets");
        int nAttributes = datasets[0].get(0).getNumDimensions();
        for (int attributeIdx = 0; attributeIdx < nAttributes; attributeIdx++) {
            float min, range;
            {
                Pair<Float,Float> minMax = getMinMaxAttributeValue(attributeIdx, datasets);
                min = minMax.getA();
                range = minMax.getB() - min;
            }
            if(range != 0){
                for(Collection<VectorSample> dataset : datasets)
                    for(VectorSample v : dataset)
                        v.set(attributeIdx, (v.get(attributeIdx) - min) / range);
            }
        }
    }
    @SafeVarargs
    private static Pair<Float,Float> getMinMaxAttributeValue(int attributeIdx, Collection<VectorSample>... datasets) {
        float minValue = Float.MAX_VALUE, maxValue = Float.MIN_VALUE;
        for(Collection<VectorSample> dataset : datasets){
            for(VectorSample v : dataset){
                float weight = v.get(attributeIdx);
                if(weight < minValue)
                    minValue = weight;
                if(weight > maxValue)
                    maxValue = weight;
            }
        }
        return new Pair<>(minValue, maxValue);
    }
    */

	/** Returns the <id,label> pair of each sample */
    public static Pair<Long,String>[] getIdsAndSamples(List<VectorSample> samples) {
        Pair<Long,String>[] ids = new Pair[samples.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = new Pair<>(samples.get(i).getId(), samples.get(i).getLabel());
        }
        return ids;
    }


    public static void concatFeatures(List<VectorSample> vectors, Map<Long, FloatVector> additionalVectors) {
        if(additionalVectors == null)
            return;
        Logs.fine("Concating features. #numAttributes=" + vectors.get(0).getNumDimensions()
            + ", #numAdditionalAttributes=" + additionalVectors.values().iterator().next().length());
        for (int i = 0; i < vectors.size(); i++) {
            VectorSample vector = vectors.get(i);
            vectors.set(i, new VectorSample(vector.getId(), vector.getLabels(),
                FloatVectorFactory.concat(vector.weights(), additionalVectors.get(vector.getId()))));
        }
    }

    public static VectorDataset load(File fileOrFolder){
    	return load(fileOrFolder, null);
    }
    public static VectorDataset load(File fileOrFolder, Consumer<VectorSample> postAction){
    	if(fileOrFolder.isFile()){
    		if(postAction != null)
    			throw new UnsupportedOperationException("postAction not yet implemented for this case");
    		return loadFromFile(fileOrFolder);
    	}
    	return loadFromFolder(new SamplePathResolverSimple(fileOrFolder), postAction);
    }

    public static Pair<VectorDataset,List<String>> loadFromFileWithAttributeNames(File datasetFile) {
	    ListCollector<VectorSample> samples = new ListCollector<>();
	    List<String> attributeNames = loadFromFile(datasetFile, -1, samples);
        return new Pair<>(new VectorDataset(samples.getElements()), attributeNames);
    }
    public static VectorDataset loadFromFile(File datasetFile) {
        return loadFromFile(datasetFile, -1);
    }
    public static VectorDataset loadFromFile(File datasetFile, int limit) {
        ListCollector<VectorSample> samples = new ListCollector<>();
        loadFromFile(datasetFile, limit, samples);
        return new VectorDataset(samples.getElements());
    }
	/** Loads the samples from a dataset file. It also returns the attribute names. */
    public static List<String> loadFromFile(File datasetFile, Collector<VectorSample> collector) {
        return loadFromFile(datasetFile, -1, collector);
    }
    public static List<String> loadFromFile(File datasetFile, int limit, Collector<VectorSample> collector) {
        Logs.finest("Loading VectorDataset from " + datasetFile);
	    TimeWatcher timeWatcher = new TimeWatcher();
        long read = 0;
        try( Scanner scanner = FileUtils.createScannerFromFile(datasetFile) ){
        	List<String> attributes = null;
        	int numAttributes;
        	{
        		String header = scanner.nextLine();
        		if(header.startsWith("DIMENSIONS=")){ //util ser assim no header quando quantidade de atributos ser REALMENTE MUITO GRANDE. Daí melhor só dizer quantidade que enumerá-los
        			numAttributes = Integer.parseInt(StringUtils.substringAfter(header, "DIMENSIONS="));
        		}else {
        			attributes = loadAttributes(header);
        			numAttributes = attributes.size();
        		}
        	}
    		while(scanner.hasNextLine()){
    			collector.collect(parseSampleLine(scanner.nextLine(), numAttributes));
    			read++;
    			if(limit > 0 && read >= limit)
    				break;
    		}
    		return attributes;
    	} finally {
    		Logs.finest("VectorDataset loaded. #samples: "+read+". After " + timeWatcher);
    	}
    }

    private static List<String> loadAttributes(String header) {
        String[] headerTokens = StringUtils.split(header, ',');
        return Arrays.asList(headerTokens).subList(2, headerTokens.length);
    }
    public static List<String> loadAttributes(File datasetFile) {
        return loadAttributes(FileUtils.readLine(datasetFile, 1));
    }

    public static SortedMap<String,MutableInt> loadDFs(File datasetFile) {
        SortedMap<String,MutableInt> termsDFs = new TreeMap<>();
        List<String> attributes = loadAttributes(datasetFile);
        loadFromFile(datasetFile, sample -> {
            sample.forEachNonZero((attrIndex,attrValue) -> {
                String attr = attributes.get(attrIndex);
                DataStructureUtils.incrementMapValue(termsDFs, attr);
            });
        });
        return termsDFs;
    }
    public static List<Long> loadIDs(File datasetFile) {
        ArrayList<Long> ids = new ArrayList();
        loadFromFile(datasetFile, sample -> ids.add(sample.getId()));
        return ids;
    }
    private static VectorSample parseSampleLine(String sampleLine, int numAttributes) {
        String[] sampleLineTokens = sampleLine.split(",");
        long id = Long.parseLong(sampleLineTokens[0]);
        String label = sampleLineTokens[1];
        boolean sparse = sampleLineTokens.length > 2 && sampleLineTokens[2].indexOf('=') > 0;

        FloatVector weights = FloatVectorFactory.create(numAttributes);
        VectorSample sample = new VectorSample(id, label, weights);

        if(sparse){ //sparse representation
            for(int i = 2; i < sampleLineTokens.length; i++){
                String indexAndValue = sampleLineTokens[i];
                int separatorIdx = indexAndValue.indexOf('=');
                int attrIndex = Integer.parseInt(indexAndValue.substring(0, separatorIdx));
                String value = indexAndValue.substring(separatorIdx+1);
                weights.set(attrIndex, Float.parseFloat(value));
            }
        }else{ //dense
            for(int i = 2; i < sampleLineTokens.length; i++)
                weights.set(i - 2, Float.parseFloat(sampleLineTokens[i]));
        }

        return sample;
    }

    public static VectorDataset loadFromFolder(File folder) {
        return loadFromFolder(new SamplePathResolverSimple(folder));
    }
    public static VectorDataset loadFromFolder(SamplePathResolver pathResolver) {
    	return loadFromFolder(pathResolver, null);
    }
    public static VectorDataset loadFromFolder(SamplePathResolver pathResolver, Consumer<VectorSample> postAction) {
        Logs.finest("Loading VectorDataset from " + pathResolver);
        TimeWatcher timeWatcher = new TimeWatcher();
        ObjectIO objectIO = new ObjectIO();
        ArrayList<VectorSample> samples = new ArrayList<>();
        Consumer<File> reader;
        if(postAction == null){
        	reader = sampleFile -> samples.add(loadSampleFromFile(sampleFile, objectIO));
        }else{
    		reader = sampleFile -> {
    			VectorSample s = loadSampleFromFile(sampleFile, objectIO);
    			postAction.accept(s);
    			samples.add(s);
    		};
    	}
		pathResolver.forEachFile(false, reader);
        Collections.sort(samples, Sample.COMPARATOR_BY_ID);
        Logs.finest("VectorDataset was read after " + timeWatcher);
        return new VectorDataset(samples);
    }
    private static VectorSample loadSampleFromFile(File file, ObjectIO objectIO) {
        try {
            return objectIO.readObjectFromFile(file);
        } catch (RuntimeException|Error e) {
            Logs.severe("Error while reading VectorSample from file " + file);
        	throw e;
    	}
    }

    public static List<VectorSample> loadFromSVMLight(File datasetFile, int featureIndexOffset, int numAttributes, IndexableSet<String> labelByIndex) {
        ListCollector<VectorSample> collector = new ListCollector<>();
        loadFromSVMLight(datasetFile, featureIndexOffset, numAttributes, labelByIndex, collector);
        return collector.getElements();
    }
    public static void loadFromSVMLight(File datasetFile, int featureIndexOffset, int numAttributes, IndexableSet<String> labelByIndex, Collector<VectorSample> collector) {
        Preconditions.checkArgument(featureIndexOffset >= 0);
        Logs.finest("Loading VectorDataset from " + datasetFile);
        TimeWatcher timeWatcher = new TimeWatcher();
        long read = 0;
        try( Scanner scanner = FileUtils.createScannerFromFile(datasetFile) ){
            while(scanner.hasNextLine()){
                String line = scanner.nextLine();

                //gets labelIndex, then shrinks the line
                int idx = line.indexOf(" ");
                int labelIndex = Integer.parseInt(line.substring(0, idx));
                line = line.substring(idx+1);

                //gets id, then shrinks the line
                idx = line.indexOf(" # ");
                long id = Long.parseLong(line.substring(idx+3));
                line = line.substring(0, idx);

                FloatVector weights = FloatVectorFactory.create(numAttributes);
                StringTokenizer tokens = new StringTokenizer(line, " ");
                while(tokens.hasMoreTokens()){
                    String indiceAndWeight = tokens.nextToken();
                    idx = indiceAndWeight.indexOf(":");
                    int index = Integer.parseInt(indiceAndWeight.substring(0, idx)) - featureIndexOffset;
                    float weight = Float.parseFloat(indiceAndWeight.substring(idx+1));
                    weights.set(index, weight);
                }

                String label = labelByIndex.getValue(labelIndex);
                Preconditions.checkState(label != null, "Label not found for index "+labelIndex);
                collector.collect(new VectorSample(id, label, weights));
                read++;
            }
        } finally {
            Logs.finest("VectorDataset loaded. #samples: "+read+". After " + timeWatcher);
        }
    }

	public void writeToFile(File outputFile, boolean includeIdAndLabel, boolean sparse) {
		writeToFile(samples, outputFile, includeIdAndLabel, sparse);
	}

	public static void writeToFile(List<VectorSample> samples, File outputFile, boolean includeIdAndLabel, boolean sparse) {
		try( MatrixWriter writer = initWriter(outputFile, includeIdAndLabel, samples.get(0).getNumDimensions(), sparse) ){
            for(VectorSample sample : samples){
                appendSample(writer, includeIdAndLabel, sparse, sample);
            }
        }
	}
	public static MatrixWriter initWriter(File outputFile, boolean includeIdAndLabel, int dimensions, boolean sparse) {
        MatrixWriter writer = new MatrixWriter(outputFile);
        if(sparse){
        	writer.add("DIMENSIONS="+dimensions);
        }else{  // dense
        	if(includeIdAndLabel){
        		writer.add(ID_COLUMN_NAME);
        		writer.separateAndAdd(LABEL_COLUMN_NAME);
        		for(int i = 1; i <= dimensions; i++)
    				writer.separateAndAdd("D" + i);
        	}else{
    			for(int i = 1; i <= dimensions; i++){
    				if(i > 1)
    					writer.separate();
    				writer.add("D" + i);
    			}
        	}
        }
        return writer;
    }
    public static void appendSample(MatrixWriter writer, boolean includeIdAndLabel, boolean sparse, VectorSample sample) {
        writer.newLine();
        if(includeIdAndLabel){
            writer.add(sample.getId());
            int numLabels = sample.getNumberLabels();
            if(numLabels > 1)
                throw new UnsupportedOperationException("multi-labeled samples are not supported yet");
            writer.separateAndAdd(numLabels == 1 ? sample.getFirstLabel() : "");
        }
        if(sparse){
            Pointer<Boolean> first = new Pointer<>(true);
            sample.forEachNonZero((i, weight) -> {
                if(includeIdAndLabel || !first.get())
                    writer.separate();
                writer.add(i+"=").addFormattedDecimal(weight);
                first.set(false);
            });
        }else{
            int dimensions = sample.getNumDimensions();
            for (int i = 0; i < dimensions; i++) {
                float weight = sample.get(i);
                if(includeIdAndLabel || i > 0)
                    writer.separate();
                writer.addFormattedDecimal(weight);
            }
        }
    }

    public static void convertBinaryFolderDatasetToFile(String originFolder, String destineFile){
		Preconditions.checkArgument(FileUtils.isFolder(originFolder));
		Preconditions.checkArgument(!FileUtils.exists(destineFile));
		ObjectIO io = new ObjectIO();
		int nAtributes = VectorDataset.loadSampleFromFile(new File(originFolder, "0"), io).getNumDimensions();
		AtomicInteger count = new AtomicInteger();
		try( MatrixWriter writer = initWriter(new File(destineFile), true, nAtributes, true) ){
			FileUtils.forEachFileWithinFolder(new File(originFolder), false, fs -> {
				VectorSample s = loadSampleFromFile(fs, io);
				appendSample(writer, true, true, s);
				System.out.println(count.incrementAndGet());
			});
		}
	}

    /**
     * Formato SVMLight:
     *   <classe> <feature1>:<peso1> <feature2>:<peso2> # <doc_id>
     * Onde:
     *   - não é preciso informar atributos de peso 0
     *   - para 'classe' e 'featureX', utiliza números ao invés de texto. Para tal, elaboramos um mapeamento valor<->indice pra cada.
     */
    public void writeAsSVMLight(File outputFile, boolean binaryWeight, int classIndexOffset, int featureIndexOffset) {
        try(PrintStream stream = FileUtils.createPrintStreamToFile(outputFile)){
            IndexableSet<String> classes = new IndexableSet<>(getLabelsInOrder(), String.class, classIndexOffset);
            DecimalFormat weightFormatter = binaryWeight ? null : FileUtils.getDecimalFormatter(null);
            for(VectorSample sample : samples){
                stream.print(classes.getIndex(sample.getLabel()));
                sample.forEachNonZero((featureIndex,weight) -> {
                    stream.print(' ');
                    stream.print(featureIndex + featureIndexOffset);
                    stream.print(':');
                    stream.print(binaryWeight ? "1" : weightFormatter.format(weight));
                });
                stream.print(" # ");
                stream.print(sample.getId());
                stream.println();
            }
        }
    }

    public Map<Long,FloatVector> getSamplesById() {
        Map<Long,FloatVector> result = new HashMap<>(samples.size());
        for (VectorSample sample : samples) {
            result.put(sample.getId(), sample.weights());
        }
        return result;
    }

    public static void main(String[] args){
    	String f = "/home/icaro/projects/fusionGraph/results/ohsumed/rankedLists-aggregated/FGvec_descriptors=bow-cosine+WMD-BoGrandom3000-SOFT-SUM-MCS";
    	convertBinaryFolderDatasetToFile(f, f+"_");
	}
}
