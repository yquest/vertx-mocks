package pt.fabm.commands;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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

@Name("service-undeploy")
@Summary("Undeploy service")
public class UndeployService extends AnnotatedCommand {
    private static Logger LOGGER = LoggerFactory.getLogger(UndeployService.class);
    private String name;

    @Argument(index = 0, argName = "name")
    @Description("the service name")
    @SuppressWarnings("unused")//used with reflexion
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void process(CommandProcess process) {
        final ServicesDeployedJson servicesDeployedJson = AppContext.getInstance()
                .getServicesDeployedJson();

        servicesDeployedJson
                .undeployService(process, name)
                .flatMap(id ->
                        servicesDeployedJson
                                .unregisterServiceDeployed(process.session(), name, id)
                )
                .onFailure(e -> {
                    if (e instanceof NoStackTraceThrowable) {
                        process.write(e.getMessage() + "\n");
                    } else {
                        process.write("error on try to undeploy\n");
                        LOGGER.error(e);
                    }
                    process.end();
                })
                .onSuccess(e -> {
                    process.write("undeployed successfully\n");
                    process.end();
                });
    }

    @Override
    public void complete(Completion completion) {
        AppContext.getInstance().getServicesDeployedJson().getDeployedIds(completion.session())
                .onFailure(e -> {
                    completion.complete("", false);
                    LOGGER.error(e);
                })
                .onSuccess(e -> {
                    CompletionList completionList = new CompletionList(1, completion);
                    completionList.handle(
                            Future.succeededFuture(
                                    new ArrayList<>(e.fieldNames())
                            )
                    );
                });


        /*
        Handler<AsyncResult<JsonObject>> handler = ar -> {
            if (ar.failed()) {
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
            handler.handle(Future.succeededFuture(completion
                            .session()
                            .get(ServicesDeployedJson.SERVICE_DEPLOY_IDS)
                    )
            );
        }*/
    }
}
