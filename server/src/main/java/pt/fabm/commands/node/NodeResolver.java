package pt.fabm.commands.node;

import java.util.Iterator;

public interface NodeResolver {
    String getName();

    NodeResolver getParent();

    String complement();

    Iterator<NodeResolver> iterator();

    NodeResolver resolve();

    boolean exists();

    boolean isDirectory();
}
