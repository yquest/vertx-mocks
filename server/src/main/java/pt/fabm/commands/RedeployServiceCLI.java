package pt.fabm.commands;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.cli.Argument;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.impl.DefaultCLI;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandBuilder;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.web.RoutingContext;
import pt.fabm.instances.Context;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pt.fabm.instances.ContextModule.PATH_TO_SAVE;

public class RedeployServiceCLI implements AppAction {
    private static Logger LOGGER = LoggerFactory.getLogger(RedeployServiceCLI.class);

    private Context context;

    @Inject
    RedeployServiceCLI(Context context) {
        this.context = context;
    }

    public Command getCommand() {
        final Argument argument = new Argument()
                .setArgName("name")
                .setRequired(true)
                .setIndex(0);
        CLI cli = context.cliProvider().get().setName("service-redeploy")
                .setDescription("Deploy service")
                .setArguments(Collections.singletonList(argument));

        return context.getCommandBuilderByCli().create(cli)
                .processHandler(this::handleProcess)
                .completionHandler(this::handleCompletion)
                .build(context.getVertx());
    }

    @Override
    public String getRoutPath() {
        return "/redeploy/:name";
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.GET;
    }

    public void routing(RoutingContext rc) {
        String name = rc.request().getParam("name");
        context
                .getServicesDeployedJson()
                .redeployJson(name)
                .onSuccess(id -> rc.response().end(new JsonObject().put("id", id).toBuffer()))
                .onFailure(error -> AppAction.onError(LOGGER, rc, error));
    }

    private void handleProcess(CommandProcess process) {
        String name = process.args().get(0);
        context
                .getServicesDeployedJson()
                .redeployJson(name)
                .onFailure(error -> AppAction.onError(process, error))
                .onSuccess(id -> {
                    process.write(String.format("Redeploy with id %s\n", id));
                    process.end();
                });
    }

    private void handleCompletion(Completion completion) {
        Handler<AsyncResult<JsonObject>> handler = ar -> {
            if (ar.failed()) {
                LOGGER.error(ar.cause());
                completion.complete("", false);
                return;
            }
            JsonObject jsonObject = ar.result();
            List<String> list;
            if (jsonObject == null) {
                list = null;
            } else {
                list = new ArrayList<>(jsonObject.fieldNames());
            }
            CompletionList completionList = new CompletionList(1, completion);
            completionList.handle(Future.succeededFuture(list));
        };

        if (PATH_TO_SAVE != null) {
            context
                    .getPathToSave()
                    .getCache()
                    .onComplete(handler);
        } else {
            handler.handle(Future.succeededFuture(completion.session().get("service-deploy-ids")));
        }
    }

}
