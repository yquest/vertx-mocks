package pt.fabm;

import io.vertx.core.Vertx;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.term.TelnetTermOptions;
import pt.fabm.commands.*;
import pt.fabm.instances.DefaultInstance;
import pt.fabm.instances.AppContext;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        DefaultInstance.setup();
        Vertx vertx = AppContext.getInstance().getVertx();
        ShellService service = ShellService.create(vertx, new ShellServiceOptions()
                .setTelnetOptions(new TelnetTermOptions().setPort(8001))
        );
        service.server().registerCommandResolver(() -> Arrays.asList(
                Command.create(vertx, LoadServices.class),
                Command.create(vertx, VerticleByGroup.class),
                Command.create(vertx, ListServices.class),
                Command.create(vertx, DeployService.class),
                Command.create(vertx, ServicesDeployed.class),
                Command.create(vertx, UndeployService.class)
        ));
        service.start();
    }
}