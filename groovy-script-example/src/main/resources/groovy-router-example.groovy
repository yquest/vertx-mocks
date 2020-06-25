import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext

logger.info('starting router script')
path = '/script/router/test/:myparam'
method = HttpMethod.GET
int x = 0;
doRequest { RoutingContext rc ->
    body = [
            'hello':rc.request().getParam('myparam'),
            'increment':x++
    ]
}