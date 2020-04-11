package pt.fabm.commands.node;

public interface NodeComplete {
    NodeComplete on(OnElementFound onElementFound);

    NodeComplete on(OnCandidates onCandidates);

    NodeComplete on(OnNotFound onNotFound);

    void execute();
}
