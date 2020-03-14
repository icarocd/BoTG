package mining.bagOfGraphs;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import com.google.common.base.Preconditions;
import mining.bagOfGraphs.sampleSubgraph.SampleSubgraph;
import mining.bagOfGraphs.sampleSubgraph.SubgraphsHandler;
import mining.textMining.bagOfWord.VectorSample;
import mining.textMining.textToGraph.model.GraphSample;
import util.Collector;
import util.DataStructureUtils;
import util.FloatVector;
import util.Logs;
import util.Pair;
import util.TimeWatcher;
import util.dataStructure.FlexibleMatrix;

public class BoGCreator {

    public static void createBoGs(SubgraphsHandler subgraphsHandler, File subgraphsDir, boolean skipMissingSamples,
        List<SampleSubgraph> codebook, TypeAssignment typeAssignment, TypePooling typePooling, Collector<VectorSample> collector,
        List<Pair<String,String>>... idsLabelsLists)
    {
        Logs.finest("Creating BoGs, with assignment "+typeAssignment+" and pooling "+typePooling);
        TimeWatcher timeWatcher = new TimeWatcher();

        for(List<Pair<String,String>> idsLabels : idsLabelsLists){
            idsLabels.parallelStream().forEach(idLabel -> {
                long sampleId = Long.parseLong(idLabel.getA());
                File sampleFile = new File(subgraphsDir, String.valueOf(sampleId));
                if (!skipMissingSamples || sampleFile.exists()) {
                    List<SampleSubgraph> subgraphs = subgraphsHandler.loadSamplesSubgraphs(sampleFile, -1);
                    VectorSample bog = createBoG(sampleId, DataStructureUtils.asSetUnit(idLabel.getB()), subgraphs, codebook, typeAssignment, typePooling);
                    synchronized (collector) {
                        collector.collect(bog);
                    }
                }
            });
        }

        Logs.finer("BoGs created after " + timeWatcher);
    }

    public static VectorSample createBoG(GraphSample sample, List<SampleSubgraph> codebook, TypeAssignment typeAssignment,
    	TypePooling typePooling, SubgraphsHandler subgraphsHandler)
    {
    	return createBoG(sample.getId(), sample.getLabels(), subgraphsHandler.extractSubgraphs(sample),
    		codebook, typeAssignment, typePooling);
    }

	public static VectorSample createBoG(long sampleId, Set<String> labels, Collection<SampleSubgraph> sampleSubgraphs,
		List<SampleSubgraph> codebook, TypeAssignment typeAssignment, TypePooling typePooling)
	{
		FloatVector bag = createBag(sampleSubgraphs, codebook, typeAssignment, typePooling);
		return new VectorSample(sampleId, labels, bag);
	}

	private static FloatVector createBag(Collection<SampleSubgraph> sampleSubGraphs,
		List<SampleSubgraph> codebook, TypeAssignment typeAssignment, TypePooling typePooling)
	{
        Preconditions.checkArgument(!codebook.isEmpty(), "Codebook can't be empty");
        Preconditions.checkArgument(!sampleSubGraphs.isEmpty(), "Subgraph list can't be empty");

		FlexibleMatrix assign = typeAssignment.assign(sampleSubGraphs, codebook);

		return typePooling.pooling(assign);
	}
}
