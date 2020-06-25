package pt.fabm.commands;

import io.vertx.core.Future;
import io.vertx.core.cli.Argument;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.impl.DefaultCLI;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.web.RoutingContext;
import pt.fabm.instances.Context;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;

public class UndeployServiceCLI implements AppAction {
    private static Logger LOGGER = LoggerFactory.getLogger(UndeployServiceCLI.class);

    private Context context;

    @Inject
    UndeployServiceCLI(Context context) {
        this.context = context;
    }

    public Command getCommand() {
        final Argument argument = new Argument()
                .setArgName("name")
                .setRequired(true)
                .setIndex(0);
        CLI cli = context.cliProvider().get().setName("service-undeploy")
                .setDescription("Undeploy service")
                .setArguments(Collections.singletonList(argument));

        return context.getCommandBuilderByCli().create(cli)
                .processHandler(this::handleProcess)
                .completionHandler(this::handleCompletion)
                .build(context.getVertx());
    }

    @Override
    public String getRoutPath() {
        return "/undeploy/:name";
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.GET;
    }

    public void routing(RoutingContext rc) {
        String name = rc.request().getParam("name");
        context
                .getServicesDeployedJson()
                .undeployService(name)
                .onSuccess(id -> rc.response().end(new JsonObject().put("result", "ok").toBuffer()))
                .onFailure(error -> AppAction.onError(LOGGER, rc, error));
    }

    private void handleProcess(CommandProcess process) {
        String name = process.args().get(0);
        final ServicesDeployedJson servicesDeployedJson = context
                .getServicesDeployedJson();

        servicesDeployedJson
                .undeployService(name)
                .onFailure(error -> AppAction.onError(process, error))
                .onSuccess(e -> {
                    process.write("undeployed successfully\n");
                    process.end();
                });
    }

    private void handleCompletion(Completion completion) {
        context.getServicesDeployedJson().getDeployedIds()
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
    }

}
