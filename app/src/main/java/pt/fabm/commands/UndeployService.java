package pt.fabm.commands;

import io.vertx.core.*;
import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;
import pt.fabm.instances.AppContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static pt.fabm.instances.AppContext.PATH_TO_SAVE;

@Name("service-undeploy")
@Summary("Undeploy service")
public class UndeployService extends AnnotatedCommand {
    private static Logger LOGGER = LoggerFactory.getLogger(UndeployService.class);
    private String name;

    @Argument(index = 0, argName = "name")
    @Description("the service name")
    @SuppressWarnings("unused")//used with reflexion
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void process(CommandProcess process) {
        Vertx vertx = process.vertx();

        Handler<AsyncResult<JsonObject>> handler = ar->{
            if(ar.failed()){
                LOGGER.error(ar.cause());
                return;
            }
            JsonObject ids = ar.result();
            if (ids == null) {
                process.write("service is not deployed\n");
                process.end();
                return;
            }
            String id = ids.getString(name);
            if (id == null) {
                process.write("service is not deployed\n");
                process.end();
                return;
            }
            vertx.undeploy(id, (arUndeploy) -> {
                if (arUndeploy.failed()) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    arUndeploy.cause().printStackTrace(pw);
                    process.write("fail to undeploy service:\n" + sw.toString());
                    removeServiceDeployed(process);
                } else {
                    removeServiceDeployed(process);
                    process.write("undeploy service successfully\n");
                }
                process.end();
            });
        };

        if(PATH_TO_SAVE == null){
            JsonObject ids = process.session().get("service-deploy-ids");
            handler.handle(Future.succeededFuture(ids));
        }else {
            AppContext.getInstance()
                    .getPathToSaveFile()
                    .getCache()
                    .onComplete(handler);
        }

    }

    private void removeServiceDeployed(CommandProcess process) {
        Promise<JsonObject> asyncServices = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            AppContext.getInstance().getPathToSaveFile().getCache().onComplete(asyncServices);
        } else {
            JsonObject ids = process.session().get("service-deploy-ids");
            asyncServices.handle(Future.succeededFuture(ids));
        }

        Handler<AsyncResult<Void>> afterWrite = e -> {
            if (e.failed()) {
                LOGGER.error(e);
            }
            process.end();
        };

        asyncServices.future().onComplete(ar -> {
            if (ar.failed()) {
                LOGGER.error(ar.cause());
                return;
            }

            JsonObject ids = ar.result();
            if (ids == null) {
                process.write("this verticle was not deployed as a service\n");
                process.end();
                return;
            }
            if (ids.remove(name) == null) {
                process.write("this verticle was not deployed as a service\n");
                process.end();
                return;
            }

            if (PATH_TO_SAVE == null) {
                afterWrite.handle(Future.succeededFuture());
            } else {
                AppContext.getInstance().getPathToSaveFile().reset();
                process.vertx().fileSystem().writeFile(PATH_TO_SAVE, ids.toBuffer(), afterWrite);
            }
        });
    }

    @Override
    public void complete(Completion completion) {

        Handler<AsyncResult<JsonObject>> handler = ar -> {
            if (ar.failed()) {
                LOGGER.error(ar.cause());
                completion.complete("",false);
                return;
            }
            JsonObject jsonObject = ar.result();
            List<String> list;
            if (jsonObject == null) {
                list = null;
            } else {
                list = new ArrayList<>(jsonObject.fieldNames());
            }
            CompletionList completionList = new CompletionList(1, completion);
            completionList.handle(Future.succeededFuture(list));
        };

        if (PATH_TO_SAVE != null) {
            AppContext.getInstance()
                    .getPathToSaveFile()
                    .getCache()
                    .onComplete(handler);
        } else {
            handler.handle(Future.succeededFuture(completion.session().get("service-deploy-ids")));
        }
    }
}
