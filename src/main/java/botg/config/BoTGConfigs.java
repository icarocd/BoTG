package botg.config;

import java.io.File;
import org.apache.commons.lang3.builder.ToStringBuilder;
import botg.baseline.graph.GraphPathHelper;
import botg.config.base.EvaluatorConfigs;
import mining.SamplePathResolver;
import mining.SamplePathResolverSimple;
import mining.bagOfGraphs.CodebookGenerator;
import mining.bagOfGraphs.CodebookGeneratorClustering;
import mining.bagOfGraphs.CodebookGeneratorFrequency;
import mining.bagOfGraphs.CodebookGeneratorRandom;
import mining.bagOfGraphs.TypeAssignment;
import mining.bagOfGraphs.TypePooling;
import mining.bagOfGraphs.sampleSubgraph.SubgraphsHandler;
import mining.bagOfGraphs.sampleSubgraph.linkedElement.LinkedElementDistances;
import mining.bagOfGraphs.sampleSubgraph.linkedElement.LinkedElementSubgraphsHandler;
import mining.clustering.Clusterer;
import mining.clustering.MeanShiftClusterer;
import mining.textMining.textToGraph.model.GraphType;
import util.Factory;
import util.Params;
import util.RandomFactory;
import util.StringUtils;
import util.ToStringStyleNotNullNoClassName;
import util.dataStructure.Matrix;

public class BoTGConfigs extends EvaluatorConfigs {

	public final long randomSeed;

    public final String graphs;

    public final SubgraphsHandler subgraphsHandler;

    private final LinkedElementDistances subgraphDistance;

    public final boolean generateGlobalCodebook;
    public final CodebookGenerator codebookGenerator;

    public final TypeAssignment assignment;
    public final TypePooling pooling;

    public BoTGConfigs(Params params) {
        super(params);

    	randomSeed = params.getLong("randomSeed", 0L);
    	RandomFactory.setSeed(randomSeed);

    	subgraphsHandler = new LinkedElementSubgraphsHandler(params.getBoolean("subgraphIncludingIncomingEdges", false));

    	final String graphsParam = StringUtils.trimToNull(params.get("graphs"));

    	subgraphDistance = LinkedElementDistances.get(params.get("subgraphDistance"), LinkedElementDistances.DIST1_FIX);
        LinkedElementDistances.initialize(subgraphDistance);

	    graphs = graphsParam;
	    if(graphs == null)
	        throw new IllegalArgumentException("required parameter is missing: graphs");

        generateGlobalCodebook = params.getBoolean("globalCodebook", true);

	    //default parameters:
        // -use all subgraphs for codebook generation
        // -use 10% of the subgraphs as seeds in meanshift
        // -50 max iterations per seed in meanshift
        // -meanshiftBandwidth: 1 (because the current subgraph model, with its distance model, produces distance 1 too often)
        // -meanshiftQuantile: null (do not estimate bandwidth, to avoid running such step. For unknown bandwidth, see recommended quantile in Meanshift class. 0.2 seemed fine here)

        //for 'maxElementsBySampleForCodebookSet', it seems that around 50-60% from average number of nodes per graph produces best results

    	boolean onlyComplexSubgraphsForCodebookSet = params.getBoolean("onlyComplexSubgraphsForCodebookSet", false);
    	int maxElementsBySampleForCodebookSet = params.getInt("maxElementsBySampleForCodebookSet", -1);
    	boolean filterCodebookSet = params.getBoolean("filterCodebookSet", true);

    	if(params.contains("frequencyBasedCodebookApproach")){
    	    String frequencyBasedCodebookApproach = params.get("frequencyBasedCodebookApproach");
    	    codebookGenerator = new CodebookGeneratorFrequency(onlyComplexSubgraphsForCodebookSet, maxElementsBySampleForCodebookSet, frequencyBasedCodebookApproach);
    	} else if (params.contains("randomCodebookSize")) {
            int codebookSize = params.getInt("randomCodebookSize");
            codebookGenerator = new CodebookGeneratorRandom(onlyComplexSubgraphsForCodebookSet, maxElementsBySampleForCodebookSet, filterCodebookSet, codebookSize);
    	} else {
    	    int maxElementsToUseFromCodebookSet = params.getInt("maxElementsToUseFromCodebookSet", -1);
            boolean prioritizeMemoryInsteadOfSpeed = params.getBoolean("prioritizeMemory", true);

            Factory<Clusterer> clustererFactory = new Factory<Clusterer>(){
                final float percentageElementsForSeeds = params.getFloat("meanshift_percentageSeeds", 0.1F);
                final int meanshiftMaxIterations = 50;
                final Float bandwidth = 1F, quantile = null; //final Float bandwidth = null, quantile = 0.3F;
                public Clusterer create(Object... params) {
                    return new MeanShiftClusterer((Matrix<Float>)params[0], percentageElementsForSeeds, meanshiftMaxIterations, bandwidth, quantile);
                }
                public String toString() { return ToStringBuilder.reflectionToString(this, ToStringStyleNotNullNoClassName.INSTANCE); }
            };

            codebookGenerator = new CodebookGeneratorClustering(onlyComplexSubgraphsForCodebookSet, maxElementsBySampleForCodebookSet,
                filterCodebookSet, maxElementsToUseFromCodebookSet, prioritizeMemoryInsteadOfSpeed, clustererFactory);
    	}

        assignment = TypeAssignment.valueOf(params.assertParam("assignment"));
        pooling = TypePooling.valueOf(params.assertParam("pooling"));
    }

    public SamplePathResolver getGraphsFolder() {
    	return new SamplePathResolverSimple(GraphPathHelper.getGraphsFolder(getDatasetResultsFolder(), graphs, GraphType.RELATIVE_FREQUENCY));
	}

    public File createNewOutputFolder() {
    	return createNewOutputFolder("BoG");
    }
}
