package pt.fabm.commands;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Arrays;
import java.util.Optional;

public interface AppAction {

    static void onError(CommandProcess process, Throwable throwable) {
        Arrays.stream(throwable.getStackTrace())
                .forEach(stackTraceElement ->
                        process.write(stackTraceElement.toString()).write("\n")
                );
        process.write("\n");
    }

    static void onError(Logger logger, RoutingContext rc, Throwable throwable) {
        String message = Optional.ofNullable(throwable.getMessage()).orElse("unknown error");
        if (throwable instanceof NoStackTraceThrowable) {
            rc.response().end(new JsonObject().put("error", message).toBuffer());
            logger.error(message);
        } else {
            rc.response().setStatusCode(500);
            rc.response().end(new JsonObject().put("error", message).toBuffer());
            logger.error(message, throwable);
        }
    }

    Command getCommand();

    String getRoutPath();

    HttpMethod getHttpMethod();

    void routing(RoutingContext rc);
}
