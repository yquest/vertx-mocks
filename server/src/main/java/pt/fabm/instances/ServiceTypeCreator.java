package pt.fabm.instances;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public interface ServiceTypeCreator {
    boolean matchCreator(JsonObject jsonObject);

    Future<String> creteVerticle(Context context);
}
