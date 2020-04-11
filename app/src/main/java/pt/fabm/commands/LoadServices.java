package pt.fabm.commands;

import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;
import pt.fabm.commands.node.*;
import pt.fabm.instances.AppContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Name(LoadServices.SERVICES_LOAD)
@Summary("Load services from a json node")
public class LoadServices extends AnnotatedCommand {
    private static Logger LOGGER = LoggerFactory.getLogger(LoadServices.class);
    static final String SERVICES_LOAD = "services-load";
    private String path;

    @Argument(index = 0, argName = "path")
    @Description("the verticle isolationGroup")
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void process(CommandProcess process) {
        Vertx vertx = process.vertx();
        String cwd = Optional.<String>ofNullable(process.session().get("cwd")).orElse(".");
        Path completePath = Paths.get(cwd, process.args().get(0));

        if (!completePath.toFile().exists()) {
            process.write("path " + path + "doesn't exists in current context\n");
            process.end();
            return;
        }

        vertx.fileSystem().readFile(completePath.toFile().getAbsolutePath(), ar -> {
            if (ar.failed()) {
                LOGGER.error("load service node " + path + " fails");
            } else {
                process.session().put("services", ar.result().toJsonArray());
            }
            process.end();
        });
    }

    @Override
    public void complete(Completion completion) {
        final String path = AppContext.getInstance().getTokenGetter().getToken(1,completion);
        OnCandidates candidates = completion::complete;
        OnNotFound notFound = () -> completion.complete("", false);
        OnElementFound found = e -> completion.complete(e, false);
        AppContext.getInstance()
                .getNodeComplete(path)
                .on(candidates)
                .on(notFound)
                .on(found)
                .execute();
    }
}
