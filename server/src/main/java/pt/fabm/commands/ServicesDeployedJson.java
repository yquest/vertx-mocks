package pt.fabm.commands;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import pt.fabm.instances.Context;
import pt.fabm.instances.ServiceTypeCreator;

import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Objects;

import static pt.fabm.instances.ContextModule.PATH_TO_SAVE;


public interface ServicesDeployedJson {

    Context getContext();


    default Future<String> redeployJson(String name) {
        return undeployService(name)
                .flatMap(e -> deployFromServices(name));
    }

    default Future<Void> undeployService(String name) {
        Promise<Void> undeployVericle = Promise.promise();
        return getContext().getServicesDeployedJson().getDeployedIds()
                .flatMap(ids -> Context.require(ids, "ids map is empty"))
                .map(ids -> ids.getString(name))
                .flatMap(id -> Context.require(
                        id,
                        String.format("There is no '%s' service deployed", name)
                ))
                .flatMap(id -> {
                    getContext().getVertx().undeploy(id, undeployVericle);
                    return undeployVericle.future().map(ignore -> id);
                })
                .flatMap(id -> unregisterServiceDeployed(name, id));
    }


    default Future<JsonObject> getDeployedIds() {
        Promise<JsonObject> asyncServices = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            getContext().getPathToSave().getCache().onComplete(asyncServices);
        } else {
            asyncServices.fail(String.format("expected %s exists", PATH_TO_SAVE));
        }
        return asyncServices.future();
    }

    default Future<String> serviceJsonDeploy(JsonObject service) {
        Objects.requireNonNull(service);

        final Iterator<ServiceTypeCreator> iterator = getContext().getServiceTypeCreatorIterator();

        ServiceTypeCreator serviceTypeCreator = null;
        while (iterator.hasNext()) {
            serviceTypeCreator = iterator.next();
            if (serviceTypeCreator.matchCreator(service)) {
                break;
            }
        }
        assert serviceTypeCreator != null;
        return serviceTypeCreator.creteVerticle(getContext());
    }

    default Future<Void> unregisterServiceDeployed(String name, String id) {
        Promise<JsonObject> asyncFileRead = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            Promise<Buffer> bufferRead = Promise.promise();
            getContext().getVertx().fileSystem().readFile(PATH_TO_SAVE, bufferRead);
            bufferRead.future()
                    .map(Buffer::toJsonObject)
                    .onComplete(asyncFileRead);
        } else {
            asyncFileRead.fail(String.format("expected file %s", PATH_TO_SAVE));
        }

        Promise<Void> promise = Promise.promise();
        asyncFileRead.future().onFailure(promise::fail);
        asyncFileRead.future().flatMap(ids -> {
            if (!ids.containsKey(name)) {
                return Future.failedFuture(
                        String.format("there is no '%s' service to undeploy", name)
                );
            }
            return Future.succeededFuture(ids);
        }).onSuccess(ids -> {
            if (!ids.getString(name).equals(id)) {
                promise.fail(String.format("the id[%s] doesn't corresponds to the name %s", id, name));
            }
            ids.remove(name);
            getContext().getVertx().fileSystem().writeFile(PATH_TO_SAVE, ids.toBuffer(), promise);
        });
        return promise.future();
    }

    default Future<Void> registerServiceDeployed(String name, String id) {
        Promise<JsonObject> asyncFileRead = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            Promise<Buffer> bufferRead = Promise.promise();
            getContext().getVertx().fileSystem().readFile(PATH_TO_SAVE, bufferRead);
            bufferRead.future()
                    .map(Buffer::toJsonObject)
                    .onComplete(asyncFileRead);
        } else {
            asyncFileRead.fail("path file doesn't exists:" + PATH_TO_SAVE);
        }

        Promise<Void> promise = Promise.promise();
        asyncFileRead.future().onFailure(promise::fail);
        asyncFileRead.future().onSuccess(ids -> {
            ids.put(name, id);
            getContext().getVertx().fileSystem().writeFile(PATH_TO_SAVE, ids.toBuffer(), promise);
        });
        return promise.future().map(ignore -> {
            getContext().getPathToSave().reset();
            return null;
        });
    }

    default Future<JsonObject> getDeployedServices() {
        Promise<JsonObject> asyncFileRead = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            Promise<Buffer> bufferRead = Promise.promise();
            getContext().getVertx().fileSystem().readFile(PATH_TO_SAVE, bufferRead);
            bufferRead.future()
                    .map(Buffer::toJsonObject)
                    .onComplete(asyncFileRead);
        } else {
            return Future.failedFuture("expected at least an empty file");
        }

        Promise<JsonObject> idsPromise = Promise.promise();

        asyncFileRead.future().onComplete(e -> {
            if (e.failed()) {
                idsPromise.fail(e.cause());
                return;
            }
            idsPromise.complete(e.result());
        });
        return idsPromise.future();
    }

    default Future<String> deployFromServices(String name) {
        JsonArray services = getContext().getVerticleList();
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
            return Future.failedFuture("service with name " + name + " not found\n");
        }

        final Future<String> serviceJsonDeploy = serviceJsonDeploy(service);
        return serviceJsonDeploy.flatMap(id -> registerServiceDeployed(name, id)
                .map(e -> id)
        );
    }
}
