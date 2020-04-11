package pt.fabm.commands.node;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class FileWrapper implements NodeComplete {
    private File file;
    private String complement;
    private OnElementFound onElementFound;
    private OnCandidates onCandidates;
    private OnNotFound onNotFound;

    public FileWrapper(String path) {
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

    @Override
    public NodeComplete on(OnElementFound onElementFound) {
        this.onElementFound = onElementFound;
        return this;
    }

    @Override
    public NodeComplete on(OnCandidates onCandidates) {
        this.onCandidates = onCandidates;
        return this;
    }

    @Override
    public NodeComplete on(OnNotFound onNotFound) {
        this.onNotFound = onNotFound;
        return this;
    }

    @Override
    public void execute() {
        File[] array = file.listFiles();
        if (array == null) {
            onNotFound.execute();
            return;
        }
        List<File> filtered = Arrays.stream(array)
                .filter(f -> f.getName().startsWith(complement))
                .collect(Collectors.toList());

        if (filtered.size() == 1) {
            File current = filtered.get(0);
            String toComplete = current.getName().substring(complement.length());
            if (current.exists() && current.isDirectory()) {
                toComplete += "/";
            }
            onElementFound.execute(toComplete);
            return;
        }
        List<String> candidates = Arrays.stream(array)
                .filter(e -> e.getName().startsWith(complement))
                .map(File::getName)
                .collect(Collectors.toList());

        int commonChars = complement.length() - 1;
        boolean sameChar = true;
        do {
            Iterator<String> iterator = candidates.iterator();
            if (!iterator.hasNext()) {
                File current = new File(file, complement);
                if (current.exists() && current.isDirectory()) {
                    onElementFound.execute("/");
                    return;
                }
                onNotFound.execute();
                return;
            }
            String candidate = iterator.next();
            commonChars++;
            if (commonChars + 1 > candidate.length()) {
                break;
            }
            do {
                String nextCandidate = iterator.next();
                if (commonChars + 1 > nextCandidate.length()) {
                    sameChar = false;
                    break;
                }
                if (nextCandidate.charAt(commonChars) != candidate.charAt(commonChars)) {
                    sameChar = false;
                    break;
                }
            } while (iterator.hasNext());
        } while (sameChar);
        String candidate = candidates.get(0);
        if (commonChars > complement.length()) {
            onElementFound.execute(candidate.substring(complement.length(), commonChars));
        } else {
            onCandidates.execute(candidates);
        }

    }
}
