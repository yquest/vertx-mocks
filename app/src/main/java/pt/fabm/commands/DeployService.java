package pt.fabm.commands;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;
import pt.fabm.instances.AppContext;

import java.io.File;
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
        AppContext.getInstance().getServicesDeployedJson().deployFromServices(process,name)
                .onFailure(e->{
                    if (e instanceof NoStackTraceThrowable){
                        process.write(e.getMessage()+"\n");
                    }else {
                        process.write("error on deploy\n");
                        LOGGER.error(e);
                    }
                    process.end();
                })
                .onSuccess(id->{
                    process.write(String.format("successfully deployed service %s\n",id));
                    process.end();
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
