package pt.fabm.commands;

import io.vertx.core.cli.CLI;
import io.vertx.core.cli.impl.DefaultCLI;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.Deployment;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.web.RoutingContext;
import pt.fabm.instances.Context;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VerticleByGroup implements AppAction {

    private Context context;

    @Inject
    VerticleByGroup(Context context) {
        this.context = context;
    }

    public Command getCommand() {
        final io.vertx.core.cli.Argument argument = new io.vertx.core.cli.Argument()
                .setArgName("group")
                .setRequired(true)
                .setIndex(0);
        CLI cli = context.cliProvider().get().setName("verticle-ls-group")
                .setDescription("List verticles by isolationGroup")
                .setArguments(Collections.singletonList(argument));

        return context.getCommandBuilderByCli().create(cli)
                .processHandler(this::handleProcess)
                .completionHandler(this::handleCompletion)
                .build(context.getVertx());
    }

    @Override
    public String getRoutPath() {
        return "/group-ls/:group";
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.GET;
    }

    private JsonArray verticlesArray(String group) {
        VertxInternal vertx = (VertxInternal) context.getVertx();
        JsonArray array = new JsonArray();
        for (String id : vertx.deploymentIDs()) {
            Deployment deployment = vertx.getDeployment(id);
            if (group.equals(deployment.deploymentOptions().getIsolationGroup())) {
                array.add(new JsonObject()
                        .put("id", deployment.verticleIdentifier())
                        .put("options", deployment.deploymentOptions().toJson())
                );
            }
        }
        return array;
    }

    public void routing(RoutingContext rc) {
        String group = rc.request().getParam("group");
        rc.response().end(verticlesArray(group).toBuffer());
    }

    private void handleProcess(CommandProcess process) {
        String group = process.args().get(0);
        process.write(verticlesArray(group).encodePrettily()).write("\n");
        process.end();
    }

    private void handleCompletion(Completion completion) {
        VertxInternal vertx = (VertxInternal) completion.vertx();
        List<String> groups = new ArrayList<>();
        for (String id : vertx.deploymentIDs()) {
            Deployment deployment = vertx.getDeployment(id);
            String cGroup = deployment.deploymentOptions().getIsolationGroup();
            if (cGroup != null && cGroup.startsWith(completion.lineTokens().get(0).value())) {
                groups.add(cGroup);
            }
        }
        if (groups.size() > 1) {
            completion.complete(groups);
        } else if (groups.size() == 1) {
            completion.complete(groups.get(0), false);
        } else {
            completion.complete("", false);
        }
    }
}
