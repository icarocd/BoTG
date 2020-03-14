package botg.config.base;

import java.io.File;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import com.google.common.base.Preconditions;
import botg.dataset.Datasets;
import mining.DatasetCrossFold;
import mining.DatasetReader;
import mining.textMining.TextSample;
import util.Params;
import util.StringUtils;
import util.ToStringStyleNotNullMultiLine;

public class DatasetConfigs {

    private transient final String rootFolder;
    public final Datasets dataset;
    public final String foldsDirname;

    public DatasetConfigs(Object... keyAndValueParams) {
        this(new Params(keyAndValueParams));
    }
    public DatasetConfigs(Params params) {
        rootFolder = params.get("rootFolder", "/home/icaro/projects/BoTG");
        dataset = Datasets.getById(params.assertParam("dataset"));
        foldsDirname = params.get("foldsDirname"); // foldDistributions_all / foldDistributions_selection
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public String getResultsFolder(){
    	return getRootFolder() + "/results";
    }

    public String getDatasetResultsFolder() {
        return getResultsFolder() + "/" + dataset.id;
    }

	public DatasetReader<TextSample> createDatasetReader() {
    	return dataset.createDatasetReader(getRootFolder());
    }

    public DatasetCrossFold getDatasetCrossFold() {
		return DatasetCrossFold.loadOnFolder(getFoldDistributionsDir());
	}

	public File getFoldDistributionsDir() {
		return new File(getDatasetResultsFolder(), getFoldsDirname());
	}

	private String getFoldsDirname() {
	    Preconditions.checkState(StringUtils.isNotBlank(foldsDirname), "Parameter foldsDirname was not set");
        return foldsDirname;
    }

    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyleNotNullMultiLine.INSTANCE, false);
    }
}