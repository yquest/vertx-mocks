package pt.fabm;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

import java.util.function.Function;

public class CachedReadFile<T> {
    private Vertx vertx;
    private long delay;
    private String path;
    private Function<Buffer, T> converter;
    private Future<T> cache;
    private long start;

    public CachedReadFile(Vertx vertx, long delay, String path, Function<Buffer, T> converter) {
        this.vertx = vertx;
        this.delay = delay;
        this.path = path;
        this.converter = converter;
        reset();
    }

    public Future<T> getCache() {
        long now = System.currentTimeMillis();
        if (now > start + delay) {
            start = now;
            Promise<Buffer> reader = Promise.promise();
            cache = reader.future().map(converter);
            vertx.fileSystem().readFile(path, reader);
        }
        return cache;
    }

    public void reset() {
        long now = System.currentTimeMillis();
        start = now - delay - 1000;
    }

}
