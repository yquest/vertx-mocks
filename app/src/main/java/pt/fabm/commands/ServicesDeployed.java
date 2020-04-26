package pt.fabm.commands;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;

import java.nio.file.Paths;

import static pt.fabm.instances.AppContext.PATH_TO_SAVE;

@Name("services-deployed")
@Summary("show services status")
public class ServicesDeployed extends AnnotatedCommand {

    @Override
    public void process(CommandProcess process) {
        Promise<JsonObject> asyncFileRead = Promise.promise();
        if (PATH_TO_SAVE != null && Paths.get(PATH_TO_SAVE).toFile().exists()) {
            Promise<Buffer> bufferRead = Promise.promise();
            process.vertx().fileSystem().readFile(PATH_TO_SAVE, bufferRead);
            bufferRead.future()
                    .map(Buffer::toJsonObject)
                    .onComplete(asyncFileRead);
        } else {
            JsonObject ids = process.session().get(ServicesDeployedJson.SERVICE_DEPLOY_IDS);
            if (ids == null) {
                process.write("no services deployed\n");
                process.end();
            }
            asyncFileRead.handle(Future.succeededFuture(ids));
        }

        asyncFileRead.future().onComplete(e -> {
            if (e.failed()) {
                process.write("error on read file check out the log file");
                process.end();
                return;
            }
            JsonObject ids = e.result();
            if (ids == null || ids.fieldNames().isEmpty()) {
                process.write("no services deployed\n");
                process.end();
                return;
            }
            process.write(ids.encodePrettily() + "\n");
            process.end();
        });

    }
}
