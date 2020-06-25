import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

logger = LoggerFactory.getLogger('my-verticle')

void vertxStart(Future<Void> future) {
    vertx.eventBus().request('router.bus', null) { ar ->
        logger.info("loaded router")
        if (ar.succeeded()) {
            loadRouter(ar.result().body() as Router)
            future.handle(Future.succeededFuture())
        } else {
            future.handle(Future.failedFuture(ar.cause()))
            logger.error(ar.cause())
        }
    }

    logger.info "starting"
}

void loadRouter(Router router) {
    logger.info("successfully loaded router")
    route0 = router.get("/example/get/").handler({ rc ->
        logger.info('enter the route')
        HttpServerResponse response = rc.response()
        response.putHeader("content-type", "application/json")
        response.end(
                new JsonObject()
                        .put("my", "json2")
                        .toBuffer()
        )
    })

    route1 = router.route().method(HttpMethod.POST).method(HttpMethod.PUT)
    route1.path("/example/post-and-put").handler(BodyHandler.create()).handler({ rc ->
        def jsonBody = rc.bodyAsJson
        def response = rc.response()

        if (jsonBody == null || jsonBody.isEmpty()) {
            response.putHeader("content-type", "text/plain")
            response.end("to check the request payload you can send something like {\"hello\":\"world\"}")
        } else {
            response.putHeader("content-type", "application/xml")
            // Write to the response and end it
            response.end("""
                        <example>
                            <echo>${jsonBody.hello}</echo>
                        </example>""".stripIndent()
            )
        }
    })

}

void vertxStop() {
    route0.remove()
    route1.remove()
    logger.info "stopping"
}

