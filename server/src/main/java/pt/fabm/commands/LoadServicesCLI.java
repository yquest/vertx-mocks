package pt.fabm.commands;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.cli.Argument;
import io.vertx.core.cli.CLI;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.cli.CliToken;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.web.RoutingContext;
import pt.fabm.commands.node.OnCandidates;
import pt.fabm.commands.node.OnElementFound;
import pt.fabm.commands.node.OnNotFound;
import pt.fabm.instances.Context;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

public class LoadServicesCLI implements AppAction {
    private static Logger LOGGER = LoggerFactory.getLogger(LoadServicesCLI.class);
    static final String SERVICES_LOAD = "services-load";

    private Context context;

    @Inject
    LoadServicesCLI(Context context) {
        this.context = context;
    }

    public Command getCommand() {
        String PATH = "path";
        final Argument argument = new Argument()
                .setArgName(PATH)
                .setRequired(true)
                .setIndex(0);
        CLI cli = context.cliProvider().get().setName(SERVICES_LOAD)
                .setDescription("Load services")
                .setArguments(Collections.singletonList(argument));

        return context.getCommandBuilderByCli().create(cli)
                .processHandler(this::handleProcess)
                .completionHandler(this::handleCompletion)
                .build(context.getVertx());
    }

    @Override
    public String getRoutPath() {
        return "/load";
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.POST;
    }

    private Future<Void> loadContent(Promise<Buffer> content) {
        Promise<Void> result = Promise.promise();
        content.future().onSuccess(buffer -> {
            context.getVertx().sharedData()
                    .getLocalMap("services")
                    .compute("list", (key, value) -> buffer.toJsonArray());
            result.complete();
        });
        content.future().onFailure(result::fail);
        return result.future();
    }

    public void routing(RoutingContext rc) {
        rc.request().bodyHandler(buffer -> {
            final Promise<Buffer> promise = Promise.promise();
            promise.complete(buffer);
            loadContent(promise).onComplete(ar -> {
                if (ar.failed())
                    AppAction.onError(LOGGER, rc, ar.cause());
                else
                    rc.response().end(new JsonObject().put("result", "ok").toBuffer());
            });
        });
    }

    private void handleProcess(CommandProcess process) {
        String path = process.args().get(0);
        Vertx vertx = process.vertx();
        String cwd = Optional.<String>ofNullable(process.session().get("cwd")).orElse(".");
        Path completePath = Paths.get(cwd, process.args().get(0));

        if (!completePath.toFile().exists()) {
            process.write("path " + path + "doesn't exists in current context\n");
            process.end();
            return;
        }

        Promise<Buffer> promiseBuffer = Promise.promise();
        vertx.fileSystem().readFile(completePath.toFile().getAbsolutePath(), promiseBuffer);
        loadContent(promiseBuffer)
                .onComplete(ar -> {
                    if (ar.failed()) AppAction.onError(process, ar.cause());
                    else process.write("load successfully\n");
                    process.end();
                });
    }

    private void handleCompletion(Completion completion) {
        String path = completion.lineTokens().stream()
                .filter(CliToken::isText)
                .findFirst()
                .map(CliToken::value)
                .orElse(null);

        OnCandidates candidates = completion::complete;
        OnNotFound notFound = () -> completion.complete("", false);
        OnElementFound found = e -> completion.complete(e, false);

        context.getNodeComplete()
                .on(candidates)
                .on(notFound)
                .on(found)
                .execute(path);
    }

}
