package pt.fabm.commands.node;

@FunctionalInterface
public interface OnElementFound {
    void execute(String element);
}
