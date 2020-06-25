package pt.fabm;

import groovy.lang.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

import java.io.File;
import java.util.Map;

public class GroovyRouterDslVerticle extends AbstractVerticle {
    static final Logger LOGGER = LoggerFactory.getLogger(GroovyRouterDslVerticle.class);
    private final File file;
    private Route route;
    private Router router;
    private Script script;

    public GroovyRouterDslVerticle(File file) {
        this.file = file;
    }

    @Override
    public void start(Promise<Void> startPromise) {

        vertx.eventBus().<Router>request("router.bus", null, ar -> {
            if (ar.succeeded()) {
                this.router = ar.result().body();
                loadRouter(router).onComplete(startPromise);
            } else {
                startPromise.handle(Future.failedFuture(ar.cause()));
                LOGGER.error("error on router.bus", ar.cause());
            }
        });

    }

    private Future<Void> loadRouter(Router router) {
        GroovyShell shell = new GroovyShell();
        try {
            script = shell.parse(file);
            createRoute(router);
            return Future.succeededFuture();
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    private void createRoute(Router router) {
        Future<Closure<?>> rcClosure = Promise.<Closure<?>>promise().future();
        Closure<?> closure = new Closure<Object>(this) {
            @Override
            public Object call(Object... args) {
                System.out.println("consuming closure");
                rcClosure.handle(Future.succeededFuture(((Closure<?>) args[0])));
                return null;
            }
        };

        Binding binding = script.getBinding();
        binding.setVariable("logger", LOGGER);
        binding.setVariable("doRequest", closure);

        script.run();

        route = router.route((HttpMethod) binding.getVariable("method"), binding.getVariable("path").toString())
                .handler(rc -> {
                    if (rcClosure.succeeded()) {
                        rcClosure.result().call(rc);
                    }
                    final Object raw = binding.getVariable("body");
                    if (raw instanceof Map) {
                        rc.response().putHeader("content-type", "application/json");
                        rc.response().end(JsonObject.mapFrom(raw).toBuffer());
                    } else if (raw instanceof GString || raw instanceof String) {
                        rc.response().end(raw.toString());
                    }
                });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        route.remove();
        stopPromise.complete();
    }
}
