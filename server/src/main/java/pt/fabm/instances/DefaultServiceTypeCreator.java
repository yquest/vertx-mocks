package pt.fabm.instances;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.io.File;

public class DefaultServiceTypeCreator implements ServiceTypeCreator {
    private JsonObject service;

    @Override
    public boolean matchCreator(JsonObject jsonObject) {
        this.service = jsonObject;
        return true;
    }

    @Override
    public Future<String> creteVerticle(Context context) {
        Vertx vertx = context.getVertx();
        Promise<String> promise = Promise.promise();
        JsonObject jsonOptions = service.getJsonObject("options");
        DeploymentOptions deploymentOptions;
        if (jsonOptions != null) {
            deploymentOptions = new DeploymentOptions(jsonOptions);
            if (deploymentOptions.getExtraClasspath() != null) {
                for (String lPath : deploymentOptions.getExtraClasspath()) {
                    if (!new File(lPath).exists()) {
                        String msg = "the classpath " + lPath + " doesn't exist\n";
                        promise.fail(msg);
                        return promise.future();
                    }
                }
            }
        } else {
            deploymentOptions = null;
        }
        String main = service.getString("main");
        if (deploymentOptions == null) {
            vertx.deployVerticle(main, promise);
        } else {
            vertx.deployVerticle(main, deploymentOptions, promise);
        }
        return promise.future();
    }
}
