package pt.fabm.commands;

import io.vertx.core.Handler;
import io.vertx.core.cli.CLI;
import io.vertx.ext.shell.cli.CliToken;
import io.vertx.ext.shell.cli.Completion;
import io.vertx.ext.shell.command.Command;
import io.vertx.ext.shell.command.CommandBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import pt.fabm.commands.node.NodeComplete;
import pt.fabm.instances.CommandBuilderByCLI;
import pt.fabm.instances.Context;

import javax.inject.Provider;
import java.util.Collections;
import java.util.List;

public class TestCompletionPath {

    @SuppressWarnings("unchecked")
    private static <T> T uncheckedGenericMock() {
        return (T) Mockito.mock(Provider.class);
    }

    @Test
    void testEmptyCompletion() {
        //mocks
        Context contextMock = Mockito.mock(Context.class);
        CLI cliMock = Mockito.mock(CLI.class);
        Provider<CLI> cliProvider = uncheckedGenericMock();
        Mockito.when(contextMock.cliProvider()).thenReturn(cliProvider);
        CommandBuilder commandBuilderMock = Mockito.mock(CommandBuilder.class);
        Completion completionMock = Mockito.mock(Completion.class);
        Command commandMock = Mockito.mock(Command.class);
        CliToken cliToken = Mockito.mock(CliToken.class);
        List<CliToken> cliTokenList = Collections.singletonList(cliToken);
        CommandBuilderByCLI commandBuilderByCLI = cli -> commandBuilderMock;

        Mockito.when(contextMock.getCommandBuilderByCli()).thenReturn(commandBuilderByCLI);
        Mockito.when(commandBuilderMock.processHandler(Mockito.any())).thenReturn(commandBuilderMock);
        Mockito.when(commandBuilderMock.build(Mockito.any())).thenReturn(commandMock);
        Mockito.when(cliProvider.get()).thenReturn(cliMock);
        Mockito.when(cliMock.setName(LoadServicesCLI.SERVICES_LOAD)).thenReturn(cliMock);
        Mockito.when(cliMock.setDescription(Mockito.anyString())).thenReturn(cliMock);
        Mockito.when(completionMock.lineTokens()).thenReturn(cliTokenList);
        Mockito.when(cliToken.isText()).thenReturn(true);
        Mockito.when(cliToken.value()).thenReturn("text");

        Mockito.when(contextMock.getNodeCompleteFacory()).thenReturn(s -> {
            NodeComplete nodeComplete = Mockito.mock(NodeComplete.class);
            return null;
        });

        Mockito.doAnswer(e->{
            Handler<Completion> completionHandler = e.getArgument(0);
            completionHandler.handle(completionMock);
            return commandBuilderMock;
        }).when(commandBuilderMock).completionHandler(Mockito.any());

        //new LoadServicesCLI(contextMock).getCommand();

    }

}
