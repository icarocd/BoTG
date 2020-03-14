package mining.textMining.textToGraph;

import java.io.File;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.apache.commons.lang3.mutable.MutableInt;
import mining.Dataset;
import mining.Sample;
import mining.SamplePathResolver;
import mining.SamplePathResolverSimple;
import mining.textMining.textToGraph.model.GraphSample;
import util.DataStructureUtils;
import util.Logs;
import util.Pair;
import util.StringUtils;
import util.TimeWatcher;
import util.graph.DirectedWeightedLabeledGraph;
import util.graph.LabeledMeasurableGraph;
import util.graph.LabeledWeightedEdge;
import util.io.FileUtils;

public class GraphDataset extends Dataset<GraphSample> {

    public GraphDataset() {
    }

    public GraphDataset(ArrayList<GraphSample> samples) {
        super(samples);
    }

    private Map<String,MutableInt> getDocumentFrequencyFromTerms() {
        Map<String, MutableInt> termsDFs = new HashMap<>();
        for (GraphSample sample : samples) {
            Set<String> terms = sample.getGraph().vertexSet();
            for(String term : terms){
                DataStructureUtils.incrementMapValue(termsDFs, term);
            }
        }
        return termsDFs;
    }

    public GraphDatasetStats computeStatistics() {
        GraphDatasetStats stats = new GraphDatasetStats();
        for (GraphSample sample : samples) {
            LabeledMeasurableGraph graph = sample.getGraph();
            for(String term : graph.vertexSet())
                stats.incrementTermDF(term);
            for(LabeledWeightedEdge edge : graph.edgeSet())
                stats.incrementPairTermDF(edge.getSourceTarget());
        }
        return stats;
    }

    private Set<String> getTermsDFLessThan(int minimumDF) {
        Map<String, MutableInt> termsDFs = getDocumentFrequencyFromTerms();
        Set<String> unitaryTerms = new LinkedHashSet<>();
        for (Entry<String, MutableInt> termDF : termsDFs.entrySet()) {
            if(termDF.getValue().intValue() < minimumDF){
                unitaryTerms.add(termDF.getKey());
            }
        }
        return unitaryTerms;
    }

    public void normalizeWeights(float min, float max) {
        normalizeWeights(min, max, samples);
	}
	public static void normalizeWeights(float min, float max, ArrayList<GraphSample>... datasets) {
	    Logs.finer("Normalizing graph weights by dividing values for their max within each graph");
        for(ArrayList<GraphSample> samples : datasets) {
    	    for (GraphSample s : samples) {
                s.getGraph().normalizeWeights(min, max);
            }
        }
    }

    public void convertTFtoTFIDF() {
		Logs.fine("Converting graph dataset weight from TF to TF-IDF");

		GraphDatasetStats stats = computeStatistics();

		int nDocuments = samples.size();
		for (GraphSample sample : samples) {
		    LabeledMeasurableGraph graph = sample.getGraph();
			graph.convertTFtoTFIDF(nDocuments, stats);
		}
	}

    public void logStats() {
	    TreeSet<String> terms = new TreeSet<>();
	    int sumVertices = 0;
	    for(GraphSample s : samples){
	    	Set<String> graphTerms = s.getGraph().vertexSet();
            terms.addAll(graphTerms);
	    	sumVertices += graphTerms.size();
	    }
	    Logs.info("terms (" + terms.size() +"): " + terms);
	    Logs.info("Avg. #vertices per graph: " + (float)sumVertices/size());
	}

	public void pruneTermsFromSamplesByDF(int minimumTermDF, boolean reconnectOrphanEdges) {
	    if (minimumTermDF >= 2) { // menos que 2 não requer filtrarmos, resultado será igual ao de não filtrar
	        Set<String> termsToPrune = getTermsDFLessThan(minimumTermDF);
	        if (!termsToPrune.isEmpty()) {
	            Logs.fine("Pruning " + termsToPrune.size() + " terms with DF<"+minimumTermDF+" from graph samples, with reconnectOrphanEdges=" + reconnectOrphanEdges);
	            for (Iterator<GraphSample> it = samples.iterator(); it.hasNext(); ) {
                    GraphSample sample = it.next();
                    LabeledMeasurableGraph graph = sample.getGraph();
                    boolean becameEmpty = graph.prune(termsToPrune, reconnectOrphanEdges);
                    if(becameEmpty){
                        Logs.fine("Discarding sample "+ sample.getId() +" due it became empty after term prunning");
                        it.remove();
                    }
                }
	        }
	    }
    }

	public void pruneWorstWeightedTerms(int maxNodesToRetain) {
        Logs.fine("Pruning graphs to max " + maxNodesToRetain + " nodes each.");
        for (Iterator<GraphSample> it = samples.iterator(); it.hasNext(); ) {
            GraphSample sample = it.next();
            LabeledMeasurableGraph graph = sample.getGraph();
            boolean becameEmpty = graph.pruneWorstWeightedTerms(maxNodesToRetain);
            if(becameEmpty){
                Logs.fine("Discarding sample "+ sample.getId() +" due it became empty after term prunning");
                it.remove();
            }
        }
	}

	public void writeToFolder(SamplePathResolver resultFolder) {
		logSampleCountByClass();

        TimeWatcher watcher = new TimeWatcher();

        resultFolder.initialize(false);

    	for(GraphSample sample : samples)
    		writeSampleOnFolder(sample, resultFolder);

        Logs.fine("Dataset saved on folder " + resultFolder + ". Time elapsed: "+ watcher);
    }

	public static void writeSampleOnFolder(GraphSample graphSample, File resultFolder) {
		writeSampleOnFolder(graphSample, new SamplePathResolverSimple(resultFolder));
	}
	public static void writeSampleOnFolder(GraphSample graphSample, SamplePathResolver resultFolder) {
        writeSample(graphSample, getGraphSampleFile(resultFolder, graphSample.getId()));
    }
	public static void writeSample(GraphSample graphSample, File outputFile) {
//  file format:
//  id
//  labels
//  num_vertices num_edges flag_weighted
//  vertex_name vertex_weight [1 line per vertex]
//  edge_source edge_target edge_weight edge_label [1 line per edge]
	    DecimalFormat formatter = FileUtils.getDecimalFormatter(8);
	    try(PrintStream out = FileUtils.createPrintStreamToFile(outputFile)){
            out.println(graphSample.getId());
            if(graphSample.getLabels() == null)
                out.println();
            else
                out.println(StringUtils.join(graphSample.getLabels(), '\t'));
            LabeledMeasurableGraph g = graphSample.getGraph();
            Set<String> vertices = g.vertexSet();
            Set<LabeledWeightedEdge> edges = g.edgeSet();
            out.print(vertices.size());
            out.print("\t");
            out.print(edges.size());
            out.print("\t");
            out.print(g.isWeighted() ? '1' : '0');
            out.print("\n");

            for(String vertex : vertices){
                out.print(vertex);
                out.print("\t");
                Double w = g.getVertexWeight(vertex);
                if(w == null || w.isNaN())
                    throw new IllegalStateException("invalid vertex weight for ["+vertex+"] on GraphSample ["+graphSample.getId()+"]");
                out.print(formatter.format(w));
                out.print("\n");
            }
            for(LabeledWeightedEdge edge : edges){
                out.print(edge.getSource());
                out.print("\t");
                out.print(edge.getTarget());
                out.print("\t");
                double w = edge.getWeight();
                if(Double.isNaN(w))
                    throw new IllegalStateException("invalid edge weight for ["+edge+"] on GraphSample ["+graphSample.getId()+"]");
                out.print(formatter.format(w));
                out.print("\t");
                out.print(edge.getLabel());
                out.print("\n");
            }
        }
    }
	public static GraphSample loadSampleFromFile(File file) {
        try(Scanner in = FileUtils.createScannerFromFile(file)){
            long id = Long.parseLong(in.nextLine());
            Set<String> labels = null;
            {
                String s = in.nextLine();
                if(!s.isEmpty())
                    labels = DataStructureUtils.asSet(s.split("\t"));
            }
            String[] tmp = in.nextLine().split("\t");
            int nVertices = Integer.parseInt(tmp[0]);
            int nEdges = Integer.parseInt(tmp[1]);
            boolean weighted = Integer.parseInt(tmp[2])==1;
            DirectedWeightedLabeledGraph g = new DirectedWeightedLabeledGraph(weighted);
            for (int i = 1; i <= nVertices; i++) {
                tmp = in.nextLine().split("\t");
                String vertex = tmp[0];
                double weight = Double.parseDouble(tmp[1]);
                g.addVertex(vertex, weight);
            }
            for (int i = 1; i <= nEdges; i++) {
                tmp = StringUtils.splitPreserveAllTokens(in.nextLine(), '\t');
                String source = tmp[0];
                String target = tmp[1];
                double weight = Double.parseDouble(tmp[2]);
                String label = tmp[3];
                LabeledWeightedEdge edge = g.addEdge(source, target);
                edge.setWeight(weight);
                edge.setLabel(label);
            }
            return new GraphSample(id, labels, g);
        }catch (RuntimeException e) {
            Logs.severe("Error while reading GraphSample from file " + file);
            throw e;
        }
    }

    public static File getGraphSampleFile(SamplePathResolver folder, long id) {
	    return folder.getSampleFile(id, getGraphSampleFilename(id));
    }
    public static File getGraphSampleFile(String folder, long id) {
        return new File(folder, getGraphSampleFilename(id));
    }
    public static String getGraphSampleFilename(long id) {
        return id + ".graphSample";
    }
    public static long getGraphSampleId(File graphSampleFile) {
        return Sample.getIdFromFile_(graphSampleFile);
    }

    public static GraphDataset loadFromFolder(String folder) {
        return loadFromFolder(new File(folder));
    }
    public static GraphDataset loadFromFolder(File folder) {
        return loadFromFolder(new SamplePathResolverSimple(folder));
    }
    public static GraphDataset loadFromFolder(SamplePathResolver pathResolver) {
	    Logs.finest("Loading GraphDataset from " + pathResolver);
	    TimeWatcher timeWatcher = new TimeWatcher();
	    ArrayList<GraphSample> samples = new ArrayList<>();
	    forEachSampleInFolder(pathResolver, s -> samples.add(s));
	    Collections.sort(samples, Sample.COMPARATOR_BY_ID);
	    Logs.finest("GraphDataset was read after " + timeWatcher);
	    return new GraphDataset(samples);
	}
    public static void forEachSampleInFolder(File folder, Consumer<GraphSample> collector) {
    	forEachSampleInFolder(new SamplePathResolverSimple(folder), collector);
    }
    public static void forEachSampleInFolder(SamplePathResolver pathResolver, Consumer<GraphSample> collector) {
    	pathResolver.forEachFile(false, sampleFile -> collector.accept(loadSampleFromFile(sampleFile)));
    }

	public static List<GraphSample> loadSubset(String samplesDir, List<Pair<String,String>> sampleFilenamesAndLabels) {
	    List<GraphSample> samples = new ArrayList<>();
		for (Pair<String,String> sampleFilenameAndLabel : sampleFilenamesAndLabels) {
			long id = Long.parseLong(sampleFilenameAndLabel.getA());
			try {
		        samples.add(loadSampleFromFile(getGraphSampleFile(samplesDir, id)));
		    } catch (Exception e) {
		        throw new RuntimeException("Unexpected error while loading GraphSample " + id, e);
		    }
	    }
		return samples;
	}
}
