package pt.fabm.commands;

import io.vertx.core.cli.CLI;
import io.vertx.core.cli.impl.DefaultCLI;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandBuilder;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.web.RoutingContext;
import pt.fabm.instances.Context;

import javax.inject.Inject;

public class ListServicesCLI implements AppAction {
    private Context context;

    @Inject
    ListServicesCLI(Context context) {
        this.context = context;
    }

    public Command getCommand() {
        CLI cli = context.cliProvider().get().setName("services")
                .setDescription("List services");

        return context.getCommandBuilderByCli().create(cli)
                .processHandler(this::handleProcess)
                .build(context.getVertx());
    }

    @Override
    public String getRoutPath() {
        return "/";
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.GET;
    }


    public void routing(RoutingContext rc) {
        rc.response().end(context.getVerticleList().toBuffer());
    }


    private void handleProcess(CommandProcess process) {
        JsonArray services = context.getVerticleList();
        if (services.isEmpty()) {
            process.write("Services not load yet, please load with \"" + LoadServicesCLI.SERVICES_LOAD + "\" command\n");
            process.end();
            return;
        }
        process.write(services.encodePrettily() + "\n");
        process.end();
    }
}