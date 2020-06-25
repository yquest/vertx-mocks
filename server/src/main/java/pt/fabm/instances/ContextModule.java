package pt.fabm.instances;

import dagger.Module;
import dagger.Provides;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.impl.DefaultCLI;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.VertxFactory;
import io.vertx.ext.shell.command.CommandBuilder;
import pt.fabm.Application;
import pt.fabm.CachedReadFile;
import pt.fabm.commands.ActionsSelectorModule;
import pt.fabm.commands.ServicesDeployedJson;
import pt.fabm.commands.node.FileNodeComplete;
import pt.fabm.commands.node.FileNodeResolver;
import pt.fabm.commands.node.NodeComplete;
import pt.fabm.commands.node.NodeResolverFactory;
import pt.fabm.web.WebServicesServer;
import pt.fabm.web.WebServicesServerImp;

import java.io.File;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;

@Module(includes = ActionsSelectorModule.class)
public class ContextModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextModule.class);

    public static final String PATH_TO_SAVE;

    static {
        String PATH_TO_SAVE_SERVICES_MAP = "PATH_TO_SAVE_SERVICES_MAP";
        PATH_TO_SAVE = System.getProperty(PATH_TO_SAVE_SERVICES_MAP);
    }

    private final Vertx vertx;
    private final CachedReadFile<JsonObject> pathToSaveFile;
    private final NodeResolverFactory nodeResolverFactory;
    private final Deque<ServiceTypeCreator> serviceTypeCreatorDeque;

    public ContextModule() {
        vertx = ServiceHelper.loadFactory(VertxFactory.class).vertx();
        nodeResolverFactory = FileNodeResolver::new;
        pathToSaveFile = new CachedReadFile<>(
                vertx,
                1000 * 60 * 60,
                PATH_TO_SAVE,
                Buffer::toJsonObject
        );

        if (!Paths.get(PATH_TO_SAVE).toFile().exists()) {
            LOGGER.error("path doesn't exists {0}", PATH_TO_SAVE);
        }
        serviceTypeCreatorDeque = new ConcurrentLinkedDeque<>();
        serviceTypeCreatorDeque.add(new DefaultServiceTypeCreator());
    }

    private Future<JsonObject> getConfYml(String path) {
        ConfigStoreOptions configStoreOptions = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject().put("path", path));

        ConfigRetriever retriever = ConfigRetriever.create(
                vertx,
                new ConfigRetrieverOptions().addStore(configStoreOptions)
        );
        Promise<JsonObject> promise = Promise.promise();
        retriever.getConfig(promise);
        return promise.future();
    }

    @Provides
    Function<String, Future<JsonObject>> providesConfYml() {
        return this::getConfYml;
    }

    @Provides
    Vertx providesVertx() {
        return vertx;
    }

    @Provides
    CachedReadFile<JsonObject> providesPathToSaveFile() {
        return pathToSaveFile;
    }

    @Provides
    WebServicesServer providesWebServicesServer(WebServicesServerImp wss) {
        return wss;
    }

    @Provides
    Application providesApp(ApplicationImp app) {
        return app;
    }

    @Provides
    ServicesDeployedJson providesServiceDeployedJson(Context context) {
        return () -> context;
    }

    @Provides
    JsonArray getVerticlesList(Vertx vertx) {
        return vertx.sharedData()
                .<String, JsonArray>getLocalMap("services")
                .get("list");
    }

    @Provides
    public NodeResolverFactory getNodeResolverFactory() {
        return nodeResolverFactory;
    }

    @Provides
    CommandBuilderByCLI providesCommandBuilderByCLI() {
        return CommandBuilder::command;
    }

    @Provides
    CLI providesCLI() {
        return new DefaultCLI();
    }

    @Provides
    NodeComplete getNodeComplete(FileNodeComplete nodeComplete) {
        return nodeComplete;
    }

    @Provides
    ServiceTypeRegister getServiceTypeRegister() {
        return serviceTypeCreatorDeque::push;
    }

    @Provides
    Iterator<ServiceTypeCreator> getServiceTypeCreatorIterator() {
        return serviceTypeCreatorDeque.iterator();
    }
}
