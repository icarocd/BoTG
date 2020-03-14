package mining.textMining;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.LineIterator;
import util.io.FileUtils;

public class StopList {

    public static final Set<String> STOP_LIST;

    static {
        LineIterator lineIterator = null;
        try {
            HashSet<String> stopWords = new HashSet<>();

            // stopList do site http://code.google.com/p/stop-words/ version 2011.11.21, acessado em 28.12.2014
            lineIterator = FileUtils.lineIteratorOfFileFromClasspath("stop-words-english1.properties");
            while (lineIterator.hasNext()) {
            	stopWords.add(lineIterator.next());
            }

            STOP_LIST = Collections.unmodifiableSet(stopWords);
        } finally {
            LineIterator.closeQuietly(lineIterator);
        }
    }
}
