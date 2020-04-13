package pt.fabm;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.ShellServer;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.term.TelnetTermOptions;
import pt.fabm.commands.*;
import pt.fabm.instances.AppContext;
import pt.fabm.instances.DefaultInstance;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        DefaultInstance.setup();
        Vertx vertx = AppContext.getInstance().getVertx();
        ShellService service = ShellService.create(vertx, new ShellServiceOptions()
                .setTelnetOptions(new TelnetTermOptions().setPort(8001))
        );
        final ShellServer server = service.server();
        server.registerCommandResolver(() -> Arrays.asList(
                Command.create(vertx, LoadServices.class),
                Command.create(vertx, VerticleByGroup.class),
                Command.create(vertx, ListServices.class),
                Command.create(vertx, DeployService.class),
                Command.create(vertx, ServicesDeployed.class),
                Command.create(vertx, UndeployService.class)
        ));
        server.shellHandler(e -> {
            final Path path = Paths.get("conf", "verticles.json");
            if(!path.toFile().exists()){
                return;
            }
            vertx.fileSystem().readFile(path.toString(), ar -> {
                if(ar.failed()){
                    LOGGER.error(ar.cause());
                }
                e.session().put("services", ar.result().toJsonArray());
            });
        });
        service.start();
    }
}