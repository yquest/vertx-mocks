package pt.fabm;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import pt.fabm.instances.Context;
import pt.fabm.instances.ServiceTypeCreator;

import java.io.File;
import java.util.Optional;

public class GroovyDslServiceType implements ServiceTypeCreator {
    private JsonObject conf;

    @Override
    public boolean matchCreator(JsonObject jsonObject) {
        conf = jsonObject;
        return Optional.ofNullable(jsonObject.getString("service-type"))
                .filter(e -> e.equals("groovy-route-dsl"))
                .isPresent();
    }

    @Override
    public Future<String> creteVerticle(Context context) {
        Promise<String> idPromise = Promise.promise();
        JsonObject optionsJson = conf.getJsonObject("options");
        File file = new File(conf.getString("main"));
        DeploymentOptions options;
        if(optionsJson != null){
            options = new DeploymentOptions();
            options.fromJson(optionsJson);
        }else {
            options = null;
        }
        context.getVertx().deployVerticle(new GroovyRouterDslVerticle(file),options, idPromise);
        return idPromise.future();
    }
}
