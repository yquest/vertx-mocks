package pt.fabm.web;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import pt.fabm.commands.AppAction;

import java.util.List;
import java.util.Set;

public interface WebServicesServer {
    void start(int port, List<AppAction> actions, Handler<AsyncResult<HttpServer>> handler);
}
