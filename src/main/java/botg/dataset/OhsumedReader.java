package botg.dataset;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import com.google.common.base.Preconditions;
import mining.DatasetReader;
import mining.textMining.TextSample;
import util.Collector;
import util.Filter;
import util.io.FileUtils;

public class OhsumedReader implements DatasetReader<TextSample> {

    private File folder;

    public OhsumedReader(String datasetFolder) {
        this.folder = new File(datasetFolder);
        Preconditions.checkArgument(folder.isDirectory());
    }

    @Override
    public void readSamples(Filter<TextSample> sampleFilter, Collector<TextSample> collector) {
        try {
            SortedMap<Long, TreeSet<String>> id_labels = new TreeMap<>();
            SortedMap<Long, String> id_body = new TreeMap<>();

            for (File subFolder : folder.listFiles()) {
                if (subFolder.isDirectory()) {
                    String label = subFolder.getName();
                    for (File sampleFile : subFolder.listFiles()) {
                        long id = Long.parseLong(sampleFile.getName());
                        TreeSet<String> labels = id_labels.get(id);
                        if (labels == null) {
                            id_labels.put(id, labels = new TreeSet<>());
                            String text = FileUtils.readFileToString(sampleFile);
                            id_body.put(id, text);
                        }
                        labels.add(label);
                    }
                }
            }
            for (Entry<Long, TreeSet<String>> id_label : id_labels.entrySet()) {
                Long id = id_label.getKey();
                TreeSet<String> labels = id_label.getValue();
                String text = id_body.get(id);
                TextSample sample = new TextSample(id, labels, null, text, null);
                if(sampleFilter == null || sampleFilter.isAccepted(sample)){
                    collector.collect(sample);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
