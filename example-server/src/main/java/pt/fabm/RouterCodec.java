package pt.fabm;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.ext.web.Router;

public class RouterCodec implements MessageCodec<Router, Router> {
    static final String NAME = "router";

    @Override
    public void encodeToWire(Buffer buffer, Router route) {

    }

    @Override
    public Router decodeFromWire(int pos, Buffer buffer) {
        return null;
    }

    @Override
    public Router transform(Router router) {
        return router;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
