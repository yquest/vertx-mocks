package pt.fabm.commands;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.cli.CLI;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.web.RoutingContext;
import pt.fabm.instances.Context;

import javax.inject.Inject;
import java.nio.file.Paths;

import static pt.fabm.instances.ContextModule.PATH_TO_SAVE;

public class ServicesDeployedCLI implements AppAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServicesDeployedCLI.class);

    private final Context context;

    @Inject
    ServicesDeployedCLI(Context context) {
        this.context = context;
    }

    public Command getCommand() {
        CLI cli = context.cliProvider().get().setName("services-deployed")
                .setDescription("Services deployed");

        return context.getCommandBuilderByCli().create(cli)
                .processHandler(this::handleProcess)
                .build(context.getVertx());
    }

    @Override
    public String getRoutPath() {
        return "/deployed";
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.GET;
    }

    @Override
    public void routing(RoutingContext rc) {
        deployedServices()
                .onFailure(error -> AppAction.onError(LOGGER, rc, error))
                .onSuccess(ids -> rc.response().end(ids.toBuffer()));
    }


    private Future<JsonObject> deployedServices() {
        Promise<JsonObject> asyncFileRead = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            Promise<Buffer> bufferRead = Promise.promise();
            context.getVertx().fileSystem().readFile(PATH_TO_SAVE, bufferRead);
            bufferRead.future()
                    .map(Buffer::toJsonObject)
                    .onComplete(asyncFileRead);
        } else {
            return Future.failedFuture("expected at least an empty file");
        }

        Promise<JsonObject> idsPromise = Promise.promise();

        asyncFileRead.future().onComplete(e -> {
            if (e.failed()) {
                idsPromise.fail(e.cause());
                return;
            }
            idsPromise.complete(e.result());
        });
        return idsPromise.future();
    }


    private void handleProcess(CommandProcess process) {
        deployedServices().onFailure(e -> AppAction.onError(process, e));
        Promise<JsonObject> asyncFileRead = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            Promise<Buffer> bufferRead = Promise.promise();
            process.vertx().fileSystem().readFile(PATH_TO_SAVE, bufferRead);
            bufferRead.future()
                    .map(Buffer::toJsonObject)
                    .onComplete(asyncFileRead);
        } else {
            process.write("expected at least an empty file\n");
        }

        asyncFileRead.future().onComplete(e -> {
            if (e.failed()) {
                process.write("error on read file check out the log file");
                process.end();
                return;
            }
            JsonObject ids = e.result();
            if (ids == null || ids.fieldNames().isEmpty()) {
                process.write("no services deployed\n");
                process.end();
                return;
            }
            process.write(ids.encodePrettily() + "\n");
            process.end();
        });
    }

}
