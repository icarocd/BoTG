package botg.baseline.graph;

import java.io.File;
import mining.textMining.textToGraph.model.GraphType;

public class GraphPathHelper {

    public static String getGraphsFolder(String datasetResultsFolder, String graphsVersionName) {
        return datasetResultsFolder + "/" + graphsVersionName;
    }

    public static File getGraphsFolder(String datasetResultsFolder, String graphsVersionName, GraphType graphType) {
        return new File(getGraphsFolder(datasetResultsFolder, graphsVersionName) + "/" + graphType.name().toLowerCase());
    }
}
