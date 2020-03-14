package mining;
import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;
import util.io.FileUtils;

public class SamplePathResolverSimple extends SamplePathResolver {

	public SamplePathResolverSimple(File folder) {
		super(folder);
	}

	@Override
	public File getSampleFile(long id, String filename) {
		return new File(folder, filename);
	}

	@Override
	public void forEachFile(boolean parallel, Consumer<? super File> task) {
        if (parallel) {
            Arrays.stream(folder.listFiles()).parallel().forEach(task);
        } else {
            FileUtils.forEachFileWithinFolder(folder, false, task);
        }
	}
}
