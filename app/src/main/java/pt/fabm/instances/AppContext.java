package pt.fabm.instances;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import pt.fabm.CachedReadFile;
import pt.fabm.commands.ServicesDeployedJson;
import pt.fabm.commands.TokenGetter;
import pt.fabm.commands.node.NodeComplete;

import java.util.function.Function;

public class AppContext {
    private static final AppContext INSTANCE = new AppContext();
    private static String PATH_TO_SAVE_SERVICES_MAP = "PATH_TO_SAVE_SERVICES_MAP";
    public static final String PATH_TO_SAVE;

    static {
        PATH_TO_SAVE = System.getProperty(AppContext.PATH_TO_SAVE_SERVICES_MAP);
    }

    private Function<String, NodeComplete> nodeCompleteFactory;
    private TokenGetter tokenGetter;
    private CachedReadFile<JsonObject> pathToSaveFile;
    private ServicesDeployedJson servicesDeployedJson;
    private Vertx vertx;

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public static <T> Future<T> require(T obj, String message) {
        Promise<T> promise = Promise.promise();
        if (obj == null) {
            promise.fail(message);
        } else {
            promise.complete(obj);
        }
        return promise.future();
    }

    void setNodeCompleteFactory(Function<String, NodeComplete> nodeCompleteFactory) {
        this.nodeCompleteFactory = nodeCompleteFactory;
    }

    void setTokenGetter(TokenGetter tokenGetter) {
        this.tokenGetter = tokenGetter;
    }

    void setPathToSaveServices(CachedReadFile<JsonObject> pathToSaveFile) {
        this.pathToSaveFile = pathToSaveFile;
    }

    public CachedReadFile<JsonObject> getPathToSaveFile() {
        return pathToSaveFile;
    }

    public TokenGetter getTokenGetter() {
        return tokenGetter;
    }

    public NodeComplete getNodeComplete(String path) {
        return nodeCompleteFactory.apply(path);
    }

    void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public Vertx getVertx() {
        return vertx;
    }

    void setServicesDeployedJson(ServicesDeployedJson servicesDeployedJson) {
        this.servicesDeployedJson = servicesDeployedJson;
    }

    public ServicesDeployedJson getServicesDeployedJson() {
        return servicesDeployedJson;
    }
}
