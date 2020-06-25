package pt.fabm.commands.node;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class FileNodeResolver implements NodeResolver {
    private final File file;
    private final String complement;

    public FileNodeResolver(String path) {
        if (path == null || path.isEmpty()) {
            this.file = new File("./");
            complement = "";
        } else {
            if (path.endsWith("/")) {
                this.file = new File(path);
                complement = "";
            } else {
                this.file = new File(path).getAbsoluteFile().getParentFile();
                complement = new File(path).getName();
            }
        }
    }

    public FileNodeResolver(File file) {
        this.file = file;
        this.complement = "";
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public NodeResolver getParent() {
        return new FileNodeResolver(file.getParentFile());
    }

    @Override
    public Iterator<NodeResolver> iterator() {
        return Arrays.stream(Objects.requireNonNull(file.listFiles()))
                .<NodeResolver>map(FileNodeResolver::new)
                .iterator();
    }

    @Override
    public String complement() {
        return complement;
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public NodeResolver resolve() {
        return new FileNodeResolver(new File(file, complement));
    }

    @Override
    public boolean exists() {
        return file.exists();
    }
}
