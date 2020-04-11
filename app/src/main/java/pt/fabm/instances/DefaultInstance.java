package pt.fabm.instances;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import pt.fabm.CachedReadFile;
import pt.fabm.commands.TokenGetter;
import pt.fabm.commands.node.FileWrapper;
import pt.fabm.commands.node.NodeComplete;

import java.nio.file.Paths;

import static pt.fabm.instances.AppContext.PATH_TO_SAVE;

public class DefaultInstance {
    public static void setup() {
        Vertx vertx = Vertx.vertx();
        final AppContext appContext = AppContext.getInstance();
        appContext.setNodeCompleteFactory(DefaultInstance::createNodeComplete);
        final TokenGetter tokenGetter = new TokenGetter() {
        };
        appContext.setVertx(vertx);
        appContext.setTokenGetter(tokenGetter);
        if (Paths.get(PATH_TO_SAVE).toFile().exists()) {
            vertx.fileSystem().deleteBlocking(PATH_TO_SAVE);
        }
        appContext.setPathToSaveServices(new CachedReadFile<>(
                        vertx,
                        1000 * 60 * 60,
                        PATH_TO_SAVE,
                        Buffer::toJsonObject
                )
        );

    }

    private static NodeComplete createNodeComplete(String path) {
        return new FileWrapper(path);
    }


}
