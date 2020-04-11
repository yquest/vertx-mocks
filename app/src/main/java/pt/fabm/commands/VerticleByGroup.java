package pt.fabm.commands;

import io.vertx.core.cli.annotations.Argument;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.Deployment;
import io.vertx.core.impl.VertxInternal;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.AnnotatedCommand;
import io.vertx.ext.shell.command.CommandProcess;

import java.util.ArrayList;
import java.util.List;

@Name("verticle-ls-group")
@Summary("List verticles by isolationGroup")
public class VerticleByGroup extends AnnotatedCommand {

    private String group;

    @Argument(index = 0, argName = "group")
    @Description("the verticle isolationGroup")
    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public void process(CommandProcess process) {
        VertxInternal vertx = (VertxInternal) process.vertx();
        for (String id : vertx.deploymentIDs()) {
            Deployment deployment = vertx.getDeployment(id);
            if (group.equals(deployment.deploymentOptions().getIsolationGroup())) {
                process.write(id + ": " + deployment.verticleIdentifier() + ", options=" + deployment.deploymentOptions().toJson() + "\n");
            }
        }
        process.end();
    }

    @Override
    public void complete(Completion completion) {
        VertxInternal vertx = (VertxInternal) completion.vertx();
        List<String> groups = new ArrayList<>();
        for (String id : vertx.deploymentIDs()) {
            Deployment deployment = vertx.getDeployment(id);
            String cGroup = deployment.deploymentOptions().getIsolationGroup();
            if (cGroup != null && cGroup.startsWith(completion.lineTokens().get(0).value())) {
                groups.add(cGroup);
            }
        }
        if (groups.size() > 1) {
            completion.complete(groups);
        } else if (groups.size() == 1) {
            completion.complete(groups.get(0), false);
        } else {
            completion.complete("", false);
        }
    }
}
