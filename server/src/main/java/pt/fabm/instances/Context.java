package pt.fabm.instances;

import dagger.Component;
import io.vertx.core.*;
import io.vertx.core.cli.CLI;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import pt.fabm.Application;
import pt.fabm.CachedReadFile;
import pt.fabm.commands.ServicesDeployedJson;
import pt.fabm.commands.node.NodeComplete;
import pt.fabm.commands.node.NodeResolverFactory;
import pt.fabm.web.WebServicesServer;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.function.Function;

@Singleton
@Component(modules = {ContextModule.class})
public interface Context {
    Logger LOGGER = LoggerFactory.getLogger(Context.class);

    static <T> Handler<AsyncResult<T>> logOnFinish(Class<?> klass) {
        return ar -> {
            if (ar.failed()) {
                LOGGER.error("error on class:" + klass.getCanonicalName(), ar.cause());
            } else {
                LOGGER.trace("successfully finished on class:" + klass.getCanonicalName());
            }
        };
    }

    static Handler<Throwable> logOnFail() {
        return LOGGER::error;
    }

    static <T> Future<T> require(T obj, String message) {
        Promise<T> promise = Promise.promise();
        if (obj == null) {
            promise.fail(message);
        } else {
            promise.complete(obj);
        }
        return promise.future();
    }

    Vertx getVertx();

    CachedReadFile<JsonObject> getPathToSave();

    ServicesDeployedJson getServicesDeployedJson();

    WebServicesServer getWSS();

    Function<String, Future<JsonObject>> getConfYml();

    Application getApp();

    JsonArray getVerticleList();

    CommandBuilderByCLI getCommandBuilderByCli();

    Provider<CLI> cliProvider();

    NodeResolverFactory getNodeResolverFactory();

    NodeComplete getNodeComplete();

    ServiceTypeRegister getServiceRegister();

    Iterator<ServiceTypeCreator> getServiceTypeCreatorIterator();
}
