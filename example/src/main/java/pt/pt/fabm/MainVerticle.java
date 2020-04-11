package pt.pt.fabm;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

@SuppressWarnings("unused") //loaded by the class name
public class MainVerticle extends AbstractVerticle {
    private static Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        loadServerHttp().onComplete(startPromise)
                .onSuccess(e -> LOGGER.info("loaded successfully"))
                .onFailure(e -> LOGGER.error("error", e));
    }

    private Future<Void> loadServerHttp() {
        vertx.eventBus().registerCodec(new RouterCodec());
        Future<HttpServer> future = Promise.<HttpServer>promise().future();
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        vertx.eventBus().localConsumer("router.bus").handler(h -> {
            LOGGER.info("intercepted bus");
            h.reply(router, new DeliveryOptions().setCodecName(RouterCodec.NAME));
        });
        server.requestHandler(router).listen(
                config().getInteger("port"),
                config().getString("host"),
                future
        );
        return future.map(e -> null);
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        vertx.eventBus().unregisterCodec(RouterCodec.NAME);
        stopFuture.handle(Future.succeededFuture());
    }
}
