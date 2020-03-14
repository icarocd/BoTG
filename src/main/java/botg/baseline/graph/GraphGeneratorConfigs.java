package botg.baseline.graph;

import java.io.File;
import botg.config.base.ReducerConfigs;
import mining.textMining.textToGraph.model.GraphType;
import util.Params;

public class GraphGeneratorConfigs extends ReducerConfigs {

    private final String graphs;

    public final transient GraphType[] graphTypes =
    	{GraphType.RELATIVE_FREQUENCY};
		//GraphType.values();

    public final boolean FORCE_REACH; //aplica max_reach de alcance de vizinhança mesmo se tipo de grafo não suporte por padrão alcance maior que 1

    public final int MAX_REACH; //só usado para os tipos de grafo que usam reach, ou quando FORCE_REACH = true

    public final int minimumTermDF; // caso >= 1, faz prune de termos com DF menor que o indicado
    public final boolean induceEdgesAfterTermPrunning; //se prune for realizado: true faz religacao de edges, false não faz

    public final boolean useTfIdf; //OBS: modelo original nao usava tf-idf

    public final int maxNodesByGraphToRetain = -1;

    public GraphGeneratorConfigs(Params params) {
        super(params);
        graphs = params.assertParam("graphs");

        int forcedReach = params.getInt("forcedReach", 0);
        if (forcedReach > 0) {
            FORCE_REACH = true;
            MAX_REACH = forcedReach;
        } else {
            FORCE_REACH = params.getBoolean("forceReach", false);
            MAX_REACH = params.getInt("maxReach", 3);
        }

        useTfIdf = params.getBoolean("useTfIdf", true);

        minimumTermDF = params.getInt("minimumTermDF", 0);
        induceEdgesAfterTermPrunning = params.getBoolean("induceEdgesAfterTermPrunning", true);
    }

    public String getTextAsGraphRootFolder() {
        return GraphPathHelper.getGraphsFolder(getDatasetResultsFolder(), graphs);
    }

    public File getDatasetRepresentationDestineFolder(GraphType graphType) {
        return GraphPathHelper.getGraphsFolder(getDatasetResultsFolder(), graphs, graphType);
    }
}
