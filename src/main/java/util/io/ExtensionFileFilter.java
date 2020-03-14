package util.io;

import java.io.File;
import java.io.FilenameFilter;

public class ExtensionFileFilter implements FilenameFilter {

    private final String extension;

    public ExtensionFileFilter(String extension) {
        if (extension.startsWith(".")) {
            throw new IllegalArgumentException("Please pass the extension without the dot");
        }
        this.extension = "." + extension.toLowerCase();
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(extension);
    }
}
