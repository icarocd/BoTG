package util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import com.thoughtworks.xstream.XStream;

/**
 * This is thread-safe.
 */
public class ObjectIO {
    private final boolean compress;
    private final XStream xStream = new XStream();

    public ObjectIO() {
        this(true);
    }

    public ObjectIO(boolean compress){
        this.compress = compress;
    }

    public void saveObjectToFile(Object object, File sampleFile) {
        OutputStream stream = null;
        try {
            FileUtils.mkDirsForFile(sampleFile);
            stream = new FileOutputStream(sampleFile);
            if (compress) {
                stream = new GZIPOutputStream(stream);
            }
            xStream.toXML(object, stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public <T> T readObjectFromFile(File file) {
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            if (compress) {
                stream = new GZIPInputStream(stream);
            }
            return (T) xStream.fromXML(stream);
        } catch (IOException e) {
            throw new RuntimeException("error reading "+file, e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
