package pt.fabm.commands;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;
import pt.fabm.instances.AppContext;

import java.util.ArrayList;
import java.util.List;

import static pt.fabm.instances.AppContext.PATH_TO_SAVE;

@Name("service-redeploy")
@Summary("Redeploy service")
public class RedeployService extends AnnotatedCommand {
    private static Logger LOGGER = LoggerFactory.getLogger(RedeployService.class);
    private String name;

    @Argument(index = 0, argName = "name")
    @Description("the verticle name")
    @SuppressWarnings("unused") //used with reflexion
    public void setName(String name) {
        this.name = name;
    }


    @Override
    public void process(CommandProcess process) {
        AppContext.getInstance()
                .getServicesDeployedJson()
                .redeployJson(process, name)
                .onFailure(ex -> {
                    if (ex instanceof NoStackTraceThrowable) {
                        process.write(ex.getMessage() + "\n");
                    } else {
                        process.write(String.format("Error on redeploy %s check logs \n", name));
                    }
                    process.end();
                })
                .onSuccess(id -> {
                    process.write(String.format("Redeploy with id %s\n", id));
                    process.end();
                });
    }

    @Override
    public void complete(Completion completion) {
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
            AppContext.getInstance()
                    .getPathToSaveFile()
                    .getCache()
                    .onComplete(handler);
        } else {
            handler.handle(Future.succeededFuture(completion.session().get("service-deploy-ids")));
        }
    }
}
