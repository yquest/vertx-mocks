import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

HttpServer server

void vertxStart() {
    server = vertx.createHttpServer()
    Router router = Router.router(vertx)
    router.get("/example/get/").handler({ rc ->
        HttpServerResponse response = rc.response()
        response.putHeader("content-type", "application/json")
        response.end(
                new JsonObject()
                        .put("my", "json")
                        .toBuffer()
        )
    })

    Route route = router.route().method(HttpMethod.POST).method(HttpMethod.PUT)
    route.path("/example/post-and-put").handler(BodyHandler.create()).handler({ rc ->
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

    println "starting"
    server.requestHandler(router).listen(8081)
}

//stopping verticle and httpserver
void vertxStop() {
    server.close()
    println "stopping"
}

