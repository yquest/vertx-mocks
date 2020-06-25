package pt.fabm.web;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import pt.fabm.commands.AppAction;
import pt.fabm.instances.Context;

import javax.inject.Inject;
import java.util.List;

public class WebServicesServerImp implements WebServicesServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServicesServerImp.class);

    private Context context;

    @Inject
    WebServicesServerImp(Context context) {
        this.context = context;
    }

    @Override
    public void start(int port, List<AppAction> actions, Handler<AsyncResult<HttpServer>> handler) {
        HttpServer server = context.getVertx().createHttpServer();
        Router router = Router.router(context.getVertx());
        for (AppAction action : actions) {
            router.route(action.getHttpMethod(), action.getRoutPath()).handler(rc->{
                rc.response().putHeader("content-type","application/json");
                action.routing(rc);
            });
        }
        router.get("/stop").handler(this::stop);
        router.get("/alive").handler(this::alive);
        server.exceptionHandler(LOGGER::error);
        server.requestHandler(router).listen(port, Context.logOnFinish(WebServicesServerImp.class));
    }

    private void stop(RoutingContext rc) {
        rc.response().end(new JsonObject().put("result", "ending vertex in 3 seconds").toBuffer());
        LOGGER.info("ending vertx");
        context.getVertx().setTimer(3000, id -> context.getVertx().close());
    }

    private void alive(RoutingContext rc) {
        LOGGER.info("check service up");
        rc.response().end("true");
    }

}
