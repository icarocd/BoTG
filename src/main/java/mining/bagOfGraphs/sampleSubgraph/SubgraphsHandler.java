package mining.bagOfGraphs.sampleSubgraph;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import mining.SamplePathResolver;
import mining.SamplePathResolverSimple;
import mining.distance.DistanceMeasurer;
import mining.textMining.textToGraph.GraphDataset;
import mining.textMining.textToGraph.model.GraphSample;
import util.Collector;
import util.ListCollector;
import util.Logs;
import util.TimeWatcher;
import util.dataStructure.Matrix;
import util.io.FileUtils;

public abstract class SubgraphsHandler {

    public void extractAndSaveSampleSubgraphs(SamplePathResolver samplesFolder, File outputDir, boolean incremental) {
        extractAndSaveSampleSubgraphs(samplesFolder, new SamplePathResolverSimple(outputDir), incremental);
    }
	public void extractAndSaveSampleSubgraphs(SamplePathResolver samplesFolder, SamplePathResolver outputDir, boolean incremental) {
		Preconditions.checkArgument(samplesFolder.exists(), samplesFolder + " must be an existing folder");
		Logs.finest("Extracting subgraphs");
		AtomicLong completed = new AtomicLong(), existing = new AtomicLong();
        TimeWatcher logPool = new TimeWatcher();
		samplesFolder.forEachFile(true, sampleFile -> {
            File destineFile = null;
		    try {
		        long id = GraphDataset.getGraphSampleId(sampleFile);
		        destineFile = outputDir.getSampleFile(id, String.valueOf(id));
		        if(!incremental || !destineFile.exists()){
                    GraphSample sample = GraphDataset.loadSampleFromFile(sampleFile);
                    extractAndSaveSampleSubgraphs(sample, destineFile);
                    completed.incrementAndGet();
                } else {
                    existing.incrementAndGet();
                }
                if(logPool.checkSecondsSpent(60)){
                    if(incremental)
                        Logs.finest("by now: " + completed + " graphs processed; " + existing + " detected as existing");
                    else
                        Logs.finest("by now: " + completed + " graphs processed");
                }
            } catch (RuntimeException e) {
                if(!incremental)
                    throw e;
                Logs.severe(e);
                FileUtils.deleteQuietly(destineFile);
            }
		});
		Logs.finest(completed + " graphs were processed; " + existing + " detected as existing");
	}

    public void extractAndSaveSampleSubgraphs(List<GraphSample> samples, SamplePathResolver outputDir) {
    	Logs.finest("Extracting subgraphs to: " + outputDir);
    	samples.parallelStream().forEach(sample -> {
    		extractAndSaveSampleSubgraphs(sample, getSampleSubgraphsFile(sample.getId(), outputDir));
        });
	}

    private File getSampleSubgraphsFile(long id, SamplePathResolver subgraphsDir) {
        return subgraphsDir.getSampleFile(id, String.valueOf(id));
    }

	public void extractAndSaveSampleSubgraphs(GraphSample sample, File destineFile) {
		Collection<SampleSubgraph> subgraphs = extractSubgraphs(sample);
		try( Writer writer = FileUtils.createWriterToFile(destineFile) ){
			for(SampleSubgraph subgraph : subgraphs)
				writer.append(getSubgraphAsStringLine(subgraph)).append('\n');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void loadSamplesSubgraphs(File inputFile, int maxLoads, Collector<SampleSubgraph> collector) {
	    LineIterator lineIterator = FileUtils.lineIteratorOfFile(inputFile);
	    try{
	        PeekingIterator<String> peekingLineIterator = Iterators.peekingIterator(lineIterator);
            consumeMetadataFromFile(peekingLineIterator); //faz a leitura pular as linhas iniciais de metadados, se houver
            if (maxLoads < 0) { // unlimited
                while(peekingLineIterator.hasNext())
                    collector.collect(getSubgraphFromStringLine(peekingLineIterator.next()));
            } else {
                while(peekingLineIterator.hasNext() && maxLoads-- > 0)
                    collector.collect(getSubgraphFromStringLine(peekingLineIterator.next()));
            }
        } finally {
            lineIterator.close();
        }
	}
	public List<SampleSubgraph> loadSamplesSubgraphs(File inputFile, int maxLoads) {
	    ListCollector<SampleSubgraph> subgraphs = new ListCollector<>();
	    loadSamplesSubgraphs(inputFile, maxLoads, subgraphs);
	    return subgraphs.getElements();
	}
	public List<SampleSubgraph> loadSampleSubgraphs(long id, SamplePathResolver subgraphsDir) {
        return loadSamplesSubgraphs(getSampleSubgraphsFile(id, subgraphsDir), -1);
    }

    public Map<String,String> loadMetadataFromSubgraphsFile(File subgraphsFile) {
	    LineIterator lineIterator = FileUtils.lineIteratorOfFile(subgraphsFile);
	    try{
            return consumeMetadataFromFile(Iterators.peekingIterator(lineIterator));
        }finally{
            lineIterator.close();
        }
    }
	private Map<String, String> consumeMetadataFromFile(PeekingIterator<String> peekingLineIterator) {
	    LinkedHashMap<String,String> metadata = new LinkedHashMap<>();
	    while(peekingLineIterator.hasNext()){
	        if(!peekingLineIterator.peek().startsWith("#"))
	            break;
            String[] pieces = peekingLineIterator.next().substring(1).split("=");
            metadata.put(pieces[0], pieces[1]);
        }
	    return metadata;
    }

    /** computes a matrix containing the distances for each pair of given elements. Each matrix entry <i,j> refers to i and j as indices from the original list */
	public Matrix<Float> computeDistanceMatrix(List<SampleSubgraph> elements, boolean prioritizeMemoryInsteadOfSpeed) {
		return DistanceMeasurer.computeDistanceMatrix_indices(elements, prioritizeMemoryInsteadOfSpeed, true, (a,b) -> a.calculateDistance(b));
	}

    public abstract List<SampleSubgraph> extractSubgraphs(GraphSample sample);

    protected abstract CharSequence getSubgraphAsStringLine(SampleSubgraph subgraph);

    protected abstract SampleSubgraph getSubgraphFromStringLine(String sampleSubgraphAsString);

    public void append(SampleSubgraph sampleSubgraph, Writer writer) {
        append(getSubgraphAsStringLine(sampleSubgraph), writer);
    }
    public void append(CharSequence sampleSubgraphAsString, Writer writer) {
        try {
            writer.append(sampleSubgraphAsString).append('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param subgraphs
     */
    public void retainComplexSubgraphs(List<SampleSubgraph> subgraphs) {
        throw new UnsupportedOperationException("not supported yet");
    }

    /**
     * @param subgraphs
     * @param maxElements
     */
    public <T extends SampleSubgraph> List<T> reduceToMostImportant(List<T> subgraphs, int maxElements) {
        throw new UnsupportedOperationException("not supported yet");
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
