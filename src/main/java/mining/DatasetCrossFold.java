package mining;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.LineIterator;
import util.DataStructureUtils;
import util.Logs;
import util.io.FileUtils;

public class DatasetCrossFold {
	private final Map<Long,Integer> positionBySampleId; //<sampleId,position>, where position starts in 0

	private DatasetCrossFold() {
        positionBySampleId = new LinkedHashMap<>();
    }

	public static DatasetCrossFold loadOnFolder(File folder) {
        DatasetCrossFold d = new DatasetCrossFold();

        LineIterator lineIterator = FileUtils.lineIteratorOfFile(getFile(folder));
        while(lineIterator.hasNext()){
            String[] pieces = lineIterator.next().split(" ");
            d.positionBySampleId.put(Long.valueOf(pieces[0]), Integer.valueOf(pieces[1]));
        }
        FileUtils.closeQuietly(lineIterator);

        return d;
    }

    public <T extends Sample> void apply(Dataset<T> dataset) {
		Logs.finer("Applying fold distribution for dataset");

		{
			Set<Long> samplesIdsWithinFolds = positionBySampleId.keySet();
			Set<Long> samplesIds = dataset.getSampleIds();
			Set<Long> idsNotInDataset = DataStructureUtils.difference(samplesIdsWithinFolds, samplesIds);
			Set<Long> idsNotInFolds = DataStructureUtils.difference(samplesIds, samplesIdsWithinFolds);
			if (!idsNotInDataset.isEmpty() || !idsNotInFolds.isEmpty()) {
				Logs.severe("Dataset ids did not match ids from fold distribution]!"
					+ "\nElements in folds but not in dataset: " + idsNotInDataset
					+ "\nElements in dataset but not in folds: " + idsNotInFolds
					+ "\nAborting...");
				System.exit(1);
			}
		}

		Collections.sort(dataset.getSamples(), (a, b) -> {
			return positionBySampleId.get(a.getId()).compareTo(positionBySampleId.get(b.getId()));
		});
	}

	public <T extends Sample> boolean containsSample(long sampleId) {
		return positionBySampleId.containsKey(sampleId);
	}

    private static File getFile(File folder) {
        return new File(folder, "folds.sampleOrder");
    }

    public static File getTrainFoldFile(File destineFolder, int foldIndex){
        return new File(destineFolder, "fold_" + foldIndex + "_train.fold");
    }
    public static File getTestFoldFile(File destineFolder, int foldIndex){
        return new File(destineFolder, "fold_" + foldIndex + "_test.fold");
    }
}
