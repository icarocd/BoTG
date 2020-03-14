package botg.dataset;

import mining.DatasetReader;
import mining.textMining.TextSample;

public enum Datasets {
    OHSUMED("ohsumed") {
        public DatasetReader<TextSample> createDatasetReader(String rootFolder) {
            return new OhsumedReader(getDatasetFolder(rootFolder, id));
        }
    };

	public final String id;

	private Datasets(String id){
		this.id = id;
	}

	public String toString() {
		return id;
	}

	public abstract DatasetReader<TextSample> createDatasetReader(String rootFolder);

	private static String getDatasetFolder(String rootFolder, String id) {
		return rootFolder + "/datasets/" + id;
	}

    public static Datasets getById(String id) {
        for (Datasets d : values()) {
            if(d.id.equals(id))
                return d;
        }
        throw new IllegalArgumentException("Dataset unknown:"+id);
    }
}