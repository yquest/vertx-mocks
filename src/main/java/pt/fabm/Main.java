package pt.fabm;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.Deployment;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.ShellService;
import io.vertx.ext.shell.ShellServiceOptions;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandBuilder;
import io.vertx.ext.shell.command.CommandProcess;
import io.vertx.ext.shell.command.CommandRegistry;
import io.vertx.ext.shell.command.base.FileSystemLs;
import io.vertx.ext.shell.term.TelnetTermOptions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Main {

    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static void handlerCompletionMockId(Completion completion) {
        Vertx vertx = completion.vertx();
        if (completion.lineTokens().isEmpty()) {
            completion.complete("", false);
            return;
        }
        JsonArray arr = vertx.sharedData()
                .<String, JsonArray>getLocalMap("local")
                .get("mocks");
        List<String> list;
        int argSize = completion.lineTokens().size();
        if (argSize == 1) {
            list = StreamSupport
                    .stream(arr.spliterator(), false)
                    .map(Object::toString).collect(Collectors.toList());

        } else if (argSize > 1) {
            String arg = completion.lineTokens().get(1).raw();
            list = StreamSupport
                    .stream(arr.spliterator(), false)
                    .map(Object::toString)
                    .filter(c -> c.startsWith(arg))
                    .collect(Collectors.toList());
        } else {
            list = null;
        }
        if (argSize == 0) {
            completion.complete("", false);
        } else if (list.size() == 1) {
            if (completion.lineTokens().size() == 1) {
                completion.complete(list.get(0), false);
            } else {
                completion.complete(
                        list.get(0).substring(completion.lineTokens().get(1).raw().length()),
                        false
                );
            }
        } else {
            completion.complete(list);
        }

    }

    private static void handlerRedeployMock(CommandProcess process) {
        VertxInternal vertx = (VertxInternal) process.vertx();
        if (process.args().isEmpty()) {
            process.write("missing the mock\n");
            process.end();
            return;
        }

        JsonArray arr = vertx.sharedData()
                .<String, JsonArray>getLocalMap("local")
                .computeIfAbsent("mocks", k -> new JsonArray());

        String id = process.args().get(0);
        Optional<String> found = StreamSupport.stream(arr.spliterator(), false)
                .filter(c -> c.equals(id))
                .map(Object::toString)
                .findFirst();


        if (!found.isPresent()) {
            process.write("mock not found\n");
            process.end();
            return;
        }

        Promise<Void> promiseUndeploy = Promise.promise();
        Promise<String> promiseDeploy = Promise.promise();

        final Deployment deployment = vertx.getDeployment(id);
        String identifier = deployment.verticleIdentifier();
        vertx.undeploy(process.args().get(0), promiseUndeploy);
        vertx.deployVerticle(identifier, promiseDeploy);
        Future<String> done = promiseUndeploy.future().compose(v -> promiseDeploy.future());

        done.setHandler(ar -> {
            if (ar.failed()) {
                LOGGER.info("error on deploy", ar.cause());
                process.write("failed to deploy verticle\n");
                process.end();
                return;
            }
            arr.remove(id);
            arr.add(ar.result());
            vertx.sharedData()
                    .<String, JsonArray>getLocalMap("local")
                    .put("mocks", arr);
            process.write(ar.result() + "\n");
            process.end();
        });
    }

    private static void handlerDeployMock(CommandProcess process) {
        Vertx vertx = process.vertx();
        if (process.args().isEmpty()) {
            process.write("missing the mock identifier\n");
            process.end();
            return;
        }
        Promise<String> promise = Promise.promise();
        vertx.deployVerticle(process.args().get(0), promise);
        promise.future().setHandler(ar -> {
            if (ar.failed()) {
                LOGGER.fatal("error on deploy", ar.cause());
                process.write("failed to deploy verticle\n");
                process.end();
                return;
            }
            vertx.sharedData()
                    .<String, JsonArray>getLocalMap("local")
                    .computeIfAbsent("mocks", k -> new JsonArray())
                    .add(ar.result());
            process.write(ar.result() + "\n");
            process.end();
        });
    }

    private static void handlerUndeployMock(CommandProcess process) {
        Vertx vertx = process.vertx();
        if (process.args().isEmpty()) {
            process.write("missing the mock verticle\n");
            process.end();
            return;
        }
        Promise<Void> promise = Promise.promise();
        vertx.undeploy(process.args().get(0), promise);
        promise.future().setHandler(ar -> {
            if (ar.failed()) {
                LOGGER.error("error on undeploy", ar.cause());
                process.write("failed to undeploy mock verticle");
                process.end();
                return;
            }
            JsonArray arr = vertx
                    .sharedData()
                    .<String, JsonArray>getLocalMap("local")
                    .get("mocks");
            arr.remove(process.args().get(0));
            vertx
                    .sharedData()
                    .<String, JsonArray>getLocalMap("local").put("mocks", arr);
            process.end();
        });
    }

    private static void handlerUndeployAll(CommandProcess process) {
        Vertx vertx = process.vertx();
        if (process.args().isEmpty()) {
            process.write("missing the mock verticle\n");
            process.end();
            return;
        }
        JsonArray list = vertx.sharedData()
                .<String, JsonArray>getLocalMap("local")
                .computeIfAbsent("mocks", k -> new JsonArray());

        List<Future> futuresList = list
                .stream()
                .map(Object::toString)
                .map(id -> {
                    Promise<Void> undepoy = Promise.promise();
                    vertx.undeploy(id, undepoy);
                    return undepoy.future();
                })
                .collect(Collectors.toList());

        CompositeFuture.join(futuresList).setHandler(ar -> {
            System.out.println("after join " + ar.result().list().size());
            if (ar.failed()) {
                process.write("error on undeploy verticles");
                LOGGER.error("error on undeploy", ar.cause());
            } else {
                list.clear();
                process.write("undeploy verticles successfully");
            }
            process.write("\n");
            process.end();
        });
    }

    private static void listMocks(CommandProcess process) {
        VertxInternal vertx = (VertxInternal) process.vertx();
        JsonArray arr = vertx.sharedData()
                .<String, JsonArray>getLocalMap("local")
                .computeIfAbsent(
                        "mocks", k -> new JsonArray()
                );

        if (arr.isEmpty()) {
            process.write("no mock verticles found\n");
            process.end();
        } else {
            for (Object el : arr) {
                String id = el.toString();
                Deployment deployment = vertx.getDeployment(id);
                process.write(
                        id + ": " + deployment.verticleIdentifier() +
                                ", options=" + deployment.deploymentOptions().toJson() + "\n"
                );
            }
            process.end();
        }
    }

    public static void main(String[] args) {

        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        Vertx vertx = Vertx.vertx();
        ShellService service = ShellService.create(vertx, new ShellServiceOptions()
                .setTelnetOptions(new TelnetTermOptions().setPort(8001))
        );

        CommandRegistry registry = CommandRegistry.getShared(vertx);
        Command command = CommandBuilder.command("undeploy-all-mocks")
                .processHandler(Main::handlerUndeployAll)
                .build(vertx);
        registry.registerCommand(command);

        command = CommandBuilder.command("deploy-mock")
                .processHandler(Main::handlerDeployMock)
                .completionHandler(new FileSystemLs()::complete)
                .build(vertx);
        registry.registerCommand(command);

        command = CommandBuilder.command("undeploy-mock")
                .completionHandler(Main::handlerCompletionMockId)
                .processHandler(Main::handlerUndeployMock)
                .build(vertx);
        registry.registerCommand(command);

        command = CommandBuilder.command("redeploy-mock")
                .completionHandler(Main::handlerCompletionMockId)
                .processHandler(Main::handlerRedeployMock)
                .build(vertx);
        registry.registerCommand(command);

        command = CommandBuilder.command("list-mocks")
                .processHandler(Main::listMocks)
                .build(vertx);
        registry.registerCommand(command);

        service.start();
    }
}