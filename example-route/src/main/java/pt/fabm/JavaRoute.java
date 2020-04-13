package pt.fabm;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

@SuppressWarnings("unused") //loaded by the class name
public class JavaRoute extends AbstractVerticle {
    private static Logger LOGGER = LoggerFactory.getLogger(JavaRoute.class);
    private Route route;

    @Override
    public void start(Promise<Void> startPromise) {
        loadRouter().onComplete(startPromise)
                .onSuccess(e -> LOGGER.info("loaded successfully"))
                .onFailure(e -> LOGGER.error("error", e));
    }

    private Future<Void> loadRouter() {
        Promise<Void> promise = Promise.<Void>promise();
        vertx.eventBus().request("router.bus", null, (AsyncResult<Message<Router>> ar) -> {
                    LOGGER.info("loaded router");
                    if (ar.succeeded()) {
                        loadRoute(ar.result().body());
                        promise.handle(Future.succeededFuture());
                    } else {
                        promise.handle(Future.failedFuture(ar.cause()));
                        LOGGER.error(ar.cause());
                    }
                }
        );
        LOGGER.info("starting");
        return promise.future();
    }

    private void loadRoute(Router router) {
        LOGGER.info("successfully loaded router");
        route = router.get("/example/java/get/").handler(rc -> {
            HttpServerResponse response = rc.response();
            response.putHeader("content-type", "application/json");
            response.end(
                    new JsonObject()
                            .put("my", "json")
                            .toBuffer()
            );
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        route.remove();
        stopFuture.handle(Future.succeededFuture());
    }
}
