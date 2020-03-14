package mining;

import util.Collector;
import util.Filter;

public interface DatasetReader<SAMPLETYPE extends Sample> {

    void readSamples(Filter<SAMPLETYPE> sampleFilter, Collector<SAMPLETYPE> collector);

}
