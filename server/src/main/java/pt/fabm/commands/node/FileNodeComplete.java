package pt.fabm.commands.node;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FileNodeComplete implements NodeComplete {
    private NodeResolver nodeResolver;
    private OnElementFound onElementFound;
    private OnCandidates onCandidates;
    private OnNotFound onNotFound;
    private final NodeResolverFactory nodeResolverFactory;

    @Inject
    public FileNodeComplete(NodeResolverFactory nodeResolverFactory) {
        this.nodeResolverFactory = nodeResolverFactory;
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
    public void execute(String path) {
        nodeResolver = nodeResolverFactory.create(path);

        Iterator<NodeResolver> iteratorLv1 = nodeResolver.iterator();
        if (!iteratorLv1.hasNext()) {
            onNotFound.execute();
            return;
        }
        Iterable<NodeResolver> iterable = () -> iteratorLv1;
        List<NodeResolver> filtered = StreamSupport.stream(iterable.spliterator(), false)
                .filter(f -> f.getName().startsWith(nodeResolver.complement()))
                .collect(Collectors.toList());

        if (filtered.size() == 1) {
            NodeResolver current = filtered.get(0);
            String toComplete = current.getName().substring(nodeResolver.complement().length());
            if (current.isDirectory()) {
                toComplete += "/";
            }
            onElementFound.execute(toComplete);
            return;
        }
        List<String> candidates = filtered.stream()
                .filter(e -> e.getName().startsWith(e.complement()))
                .map(NodeResolver::getName)
                .collect(Collectors.toList());

        int commonChars = nodeResolver.complement().length() - 1;
        boolean sameChar = true;
        do {
            Iterator<String> iterator = candidates.iterator();
            if (!iterator.hasNext()) {
                NodeResolver current = nodeResolver.resolve();
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
        if (commonChars > nodeResolver.complement().length()) {
            onElementFound.execute(candidate.substring(nodeResolver.complement().length(), commonChars));
        } else {
            onCandidates.execute(candidates);
        }
    }

}
