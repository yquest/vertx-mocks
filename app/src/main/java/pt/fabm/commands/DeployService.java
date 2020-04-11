package pt.fabm.commands;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static pt.fabm.instances.AppContext.PATH_TO_SAVE;

@Name("service-deploy")
@Summary("Deploy service")
public class DeployService extends AnnotatedCommand {
    private static Logger LOGGER = LoggerFactory.getLogger(DeployService.class);
    private String name;

    @Argument(index = 0, argName = "name")
    @Description("the verticle name")
    @SuppressWarnings("unused") //used with reflexion
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void process(CommandProcess process) {
        Vertx vertx = process.vertx();
        JsonArray services = process.session().get("services");
        if (services == null) {
            process.write("Services not load yet, please load with \"" + LoadServices.SERVICES_LOAD + "\" command\n");
            process.end();
            return;
        }
        JsonObject dep = null;
        for (int i = 0; i < services.size(); i++) {
            JsonObject entry = services.getJsonObject(i);
            String cName = entry.getString("name");
            if (name.equals(cName)) {
                dep = entry;
                break;
            }
        }
        if (dep == null) {
            process.write("service with name " + name + " not found\n");
            process.end();
            return;
        }
        JsonObject jsonOptions = dep.getJsonObject("options");
        DeploymentOptions deploymentOptions = null;
        if (jsonOptions != null) {
            deploymentOptions = new DeploymentOptions(jsonOptions);
            if (deploymentOptions.getExtraClasspath() != null) {
                for (String lPath : deploymentOptions.getExtraClasspath()) {
                    if (!new File(lPath).exists()) {
                        process.write("the classpath " + lPath + " doesn't exist\n");
                        process.end();
                        return;
                    }
                }
            }
        }

        final Handler<AsyncResult<String>> asyncResultHandler = e -> {
            if (e.failed()) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.cause().printStackTrace(pw);
                process.write("fail to load verticle:\n" + sw.toString());
            } else {
                saveServiceDeployed(process, e.result());
                process.write("loaded verticle successfully: " + e.result() + "\n");
            }
            process.end();
        };

        if (deploymentOptions != null) {
            vertx.deployVerticle(
                    dep.getString("main"),
                    deploymentOptions,
                    asyncResultHandler
            );
        } else {
            vertx.deployVerticle(
                    dep.getString("main"),
                    asyncResultHandler
            );
        }
    }

    private void saveServiceDeployed(CommandProcess process, String id) {
        Promise<JsonObject> asyncFileRead = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            Promise<Buffer> bufferRead = Promise.promise();
            process.vertx().fileSystem().readFile(PATH_TO_SAVE, bufferRead);
            bufferRead.future()
                    .map(Buffer::toJsonObject)
                    .onComplete(asyncFileRead);
        } else {
            JsonObject ids = process.session().get("service-deploy-ids");
            if (ids == null) {
                ids = new JsonObject();
            }
            asyncFileRead.handle(Future.succeededFuture(ids));
        }

        Handler<AsyncResult<Void>> afterWrite = e -> {
            if (e.failed()) {
                LOGGER.error(e);
            }
            process.end();
        };

        asyncFileRead.future().onComplete(ar -> {
            if (ar.failed()) {
                LOGGER.error(ar.cause());
                return;
            }
            JsonObject ids = ar.result();
            ids.put(name, id);

            if (PATH_TO_SAVE == null) {
                process.session().put("service-deploy-ids", ids);
                afterWrite.handle(Future.succeededFuture());
            } else {
                process.vertx().fileSystem().writeFile(PATH_TO_SAVE, ids.toBuffer(), afterWrite);
            }
        });
    }

    @Override
    public void complete(Completion completion) {
        JsonArray services = completion.session().get("services");
        List<String> list;
        if (services == null) {
            list = null;
        } else {
            list = new ArrayList<>();
            for (int i = 0; i < services.size(); i++) {
                list.add(services.getJsonObject(i).getString("name"));
            }
        }
        CompletionList completionList = new CompletionList(1, completion);
        completionList.handle(Future.succeededFuture(list));
    }
}
