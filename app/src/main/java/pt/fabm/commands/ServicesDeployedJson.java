package pt.fabm.commands;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.shell.session.Session;
import pt.fabm.instances.AppContext;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

import static pt.fabm.instances.AppContext.PATH_TO_SAVE;

public interface ServicesDeployedJson {
    String SERVICE_DEPLOY_IDS = "service-deploy-ids";

    default Vertx getVertx() {
        return AppContext.getInstance().getVertx();
    }

    default Future<JsonObject> getDescriptor(Session session) {
        Promise<JsonObject> asyncFileRead = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            Promise<Buffer> bufferRead = Promise.promise();
            getVertx().fileSystem().readFile(PATH_TO_SAVE, bufferRead);
            bufferRead.future()
                    .map(Buffer::toJsonObject)
                    .onComplete(asyncFileRead);
        } else {
            JsonObject ids = session.get(SERVICE_DEPLOY_IDS);
            if (ids == null) {
                ids = new JsonObject();
            }
            asyncFileRead.handle(Future.succeededFuture(ids));
        }
        return asyncFileRead.future();
    }

    default Future<String> redeployJson(CommandProcess process, String name) {
        return undeployService(process, name)
                .flatMap(e -> deployFromServices(process, name));
    }

    default Future<String> undeploy(String id) {
        Promise<Void> promise = Promise.promise();
        getVertx().undeploy(id, promise);
        return promise.future().map(e -> id);
    }

    default Future<String> undeployService(CommandProcess process, String name) {
        return getDeployedIds(process.session())
                .flatMap(ids -> AppContext.require(ids, "ids map is empty"))
                .map(ids -> ids.getString(name))
                .flatMap(id -> AppContext.require(
                        id,
                        String.format("There is no '%s' service deployed", name)
                ))
                .flatMap(this::undeploy);
    }

    default Future<JsonObject> getDeployedIds(Session session) {
        Promise<JsonObject> asyncServices = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            AppContext.getInstance().getPathToSaveFile().getCache().onComplete(asyncServices);
        } else {
            asyncServices.complete(session.get(SERVICE_DEPLOY_IDS));
        }
        return asyncServices.future();
    }

    default Future<String> serviceJsonDeploy(JsonObject service) {
        Objects.requireNonNull(service);
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
            getVertx().deployVerticle(main, promise);
        } else {
            getVertx().deployVerticle(main, deploymentOptions, promise);
        }
        return promise.future();
    }

    default Future<Void> unregisterServiceDeployed(Session session, String name, String id) {
        Promise<JsonObject> asyncFileRead = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            Promise<Buffer> bufferRead = Promise.promise();
            getVertx().fileSystem().readFile(PATH_TO_SAVE, bufferRead);
            bufferRead.future()
                    .map(Buffer::toJsonObject)
                    .onComplete(asyncFileRead);
        } else {
            JsonObject ids = session.get(SERVICE_DEPLOY_IDS);
            if (ids == null) {
                ids = new JsonObject();
            }
            asyncFileRead.complete(ids);
        }

        Promise<Void> promise = Promise.promise();
        asyncFileRead.future().onFailure(promise::fail);
        asyncFileRead.future().onSuccess(ids -> {
            ids.remove(name);
            if (PATH_TO_SAVE == null) {
                session.put(SERVICE_DEPLOY_IDS, ids);
                promise.complete();
            } else {
                getVertx().fileSystem().writeFile(PATH_TO_SAVE, ids.toBuffer(), promise);
            }
        });
        return promise.future();
    }

    default Future<Void> registerServiceDeployed(Session session, String name, String id) {
        Promise<JsonObject> asyncFileRead = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            Promise<Buffer> bufferRead = Promise.promise();
            getVertx().fileSystem().readFile(PATH_TO_SAVE, bufferRead);
            bufferRead.future()
                    .map(Buffer::toJsonObject)
                    .onComplete(asyncFileRead);
        } else {
            JsonObject ids = session.get(SERVICE_DEPLOY_IDS);
            if (ids == null) {
                ids = new JsonObject();
            }
            asyncFileRead.complete(ids);
        }

        Promise<Void> promise = Promise.promise();
        asyncFileRead.future().onFailure(promise::fail);
        asyncFileRead.future().onSuccess(ids -> {
            ids.put(name, id);
            if (PATH_TO_SAVE == null) {
                session.put(SERVICE_DEPLOY_IDS, ids);
                promise.complete();
            } else {
                getVertx().fileSystem().writeFile(PATH_TO_SAVE, ids.toBuffer(), promise);
            }
        });
        return promise.future();
    }

    default Future<String> deployFromServices(CommandProcess process, String name) {
        Promise<String> promiseVerticleId = Promise.promise();
        JsonArray services = process.session().get("services");
        if (services == null) {
            promiseVerticleId.fail("Services not load yet, please load with \"" + LoadServices.SERVICES_LOAD + "\" command\n");
            return promiseVerticleId.future();
        }
        JsonObject service = null;
        for (int i = 0; i < services.size(); i++) {
            JsonObject entry = services.getJsonObject(i);
            String cName = entry.getString("name");
            if (name.equals(cName)) {
                service = entry;
                break;
            }
        }
        if (service == null) {
            promiseVerticleId.fail("service with name " + name + " not found\n");
            return promiseVerticleId.future();
        }


        final Future<String> serviceJsonDeploy = serviceJsonDeploy(service);
        return serviceJsonDeploy.compose(id ->
                registerServiceDeployed(process.session(), name, id)
                        .map(e -> id)
        );
    }
}
