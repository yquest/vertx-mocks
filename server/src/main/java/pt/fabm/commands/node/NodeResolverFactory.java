package pt.fabm.commands.node;

public interface NodeResolverFactory {
    NodeResolver create(String path);
}
