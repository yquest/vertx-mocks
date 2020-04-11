package pt.fabm.commands;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.cli.Completion;

import java.util.List;
import java.util.stream.Collectors;

class CompletionList implements Handler<AsyncResult<List<String>>>, AsyncResult<List<String>> {
    private static Logger LOGGER = LoggerFactory.getLogger(CompletionList.class);
    private int order;
    private Completion completion;
    private Future<List<String>> future = Promise.<List<String>>promise().future();

    CompletionList(int order, Completion completion) {
        this.order = order;
        this.completion = completion;
    }

    CompletionList(Completion completion) {
        this.order = 1;
        this.completion = completion;
    }

    private void complete(List<String> list) {
        if (list == null || list.isEmpty()) {
            completion.complete("", false);
            return;
        }
        if (completion.lineTokens().isEmpty()) {
            completion.complete("", false);
            return;
        }
        if (completion.lineTokens().size() == order) {
            completion.complete(list);
            return;
        }
        String arg = completion.lineTokens().get(order).raw();
        List<String> filtered = list
                .stream()
                .filter(e -> e.startsWith(arg))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            completion.complete("", false);
        } else if (filtered.size() > 1) {
            completion.complete(filtered);
        } else {
            completion.complete(filtered.get(0).substring(arg.length()), false);
        }
    }

    @Override
    public void handle(AsyncResult<List<String>> event) {
        if (event.failed()) {
            complete(null);
            LOGGER.error(event.cause());
        } else {
            complete(event.result());
        }
        future.handle(event);
    }

    @Override
    public List<String> result() {
        return future.result();
    }

    @Override
    public Throwable cause() {
        return future.cause();
    }

    @Override
    public boolean succeeded() {
        return future.succeeded();
    }

    @Override
    public boolean failed() {
        return future.failed();
    }
}
