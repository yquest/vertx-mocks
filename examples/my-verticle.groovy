import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

void vertxStart() {
    logger = LoggerFactory.getLogger('my-verticle')

    vertx.eventBus().request('router.bus', null) { ar ->
        logger.info("loaded router")
        if (ar.succeeded()) {
            loadRouter(ar.result().body() as Router)
        } else {
            logger.error(ar.cause())
        }
    }

    logger.info "starting"
}

void loadRouter(Router routerLocal) {
    logger.info("successfully loaded router")
    router = routerLocal
    route0 = router.get("/example/get/").handler({ rc ->
        logger.info('enter the route')
        HttpServerResponse response = rc.response()
        response.putHeader("content-type", "application/json")
        response.end(
                new JsonObject()
                        .put("my", "json")
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
    if (getBinding().hasVariable('router')) {
        route0.remove()
        route1.remove()
    }
    logger.info "stopping"
}

