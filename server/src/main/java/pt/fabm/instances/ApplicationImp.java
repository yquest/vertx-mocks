package pt.fabm.instances;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.ShellServer;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.term.TelnetTermOptions;
import pt.fabm.Application;
import pt.fabm.commands.AppAction;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplicationImp implements Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationImp.class);
    private final Context context;
    private final List<AppAction> actions;

    @Inject
    ApplicationImp(Context context, Set<AppAction> actions) {
        this.context = context;
        this.actions = new ArrayList<>(actions);
    }

    private Future<Void> load(JsonObject config) {
        return CompositeFuture.all(
                loadTelnet(config), loadServices(config), loadWebserver(config)
        ).map(e -> null);
    }

    private Future<Void> loadTelnet(JsonObject config) {
        Vertx vertx = context.getVertx();
        ShellService service = ShellService.create(vertx, new ShellServiceOptions()
                .setTelnetOptions(new TelnetTermOptions().setPort(
                        config.getJsonObject("telnet").getInteger("port")
                ))
        );
        final ShellServer server = service.server();
        server.registerCommandResolver(() -> actions.stream()
                .map(AppAction::getCommand)
                .collect(Collectors.toList())
        );

        Promise<Void> promise = Promise.promise();
        service.start(promise);

        return promise.future();
    }

    private Future<Void> loadWebserver(JsonObject config) {
        Promise<HttpServer> promise = Promise.promise();
        context.getWSS().start(
                config.getJsonObject("web").getInteger("port"),
                actions,
                promise
        );
        return promise.future().map(e -> null);
    }

    private Future<Void> loadServices(JsonObject config) {
        String path = config.getString("verticles");
        if (!Paths.get(path).toFile().exists()) {
            return Future.failedFuture("file doesn't exists");
        }

        Vertx vertx = context.getVertx();
        Promise<Buffer> promiseReadFile = Promise.promise();
        vertx.fileSystem().readFile(path, promiseReadFile);

        return promiseReadFile.future().map(Buffer::toJsonArray)
                .<Void>map(array -> {
                    vertx
                            .sharedData()
                            .getLocalMap("services")
                            .put("list", array);
                    return null;
                }).onFailure(LOGGER::error);
    }

    public void init(String confPath) {
        context.getConfYml().apply(confPath).onSuccess(config ->
                load(config).onComplete(LOGGER::error)
        );
    }


}
