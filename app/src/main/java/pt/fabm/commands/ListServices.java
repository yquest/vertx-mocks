package pt.fabm.commands;

import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;

@Name("services")
@Summary("List services from session")
public class ListServices extends AnnotatedCommand {
    private static Logger LOGGER = LoggerFactory.getLogger(ListServices.class);


    @Override
    public void process(CommandProcess process) {
        JsonArray services;
        try {
            services = process.session().get("services");
        } catch (Exception e) {
            LOGGER.error(e);
            process.write("error on try to get services check out the logs\n");
            process.end();
            return;
        }
        if (services == null) {
            process.write("Services not load yet, please load with \"" + LoadServices.SERVICES_LOAD + "\" command\n");
            process.end();
            return;
        }
        process.write(services.encodePrettily()+"\n");
        process.end();
    }
}