package pt.fabm.commands;

import io.vertx.ext.shell.cli.Completion;

public interface TokenGetter {
    default String getToken(int position, Completion completion) {
        final String token;
        if (completion.lineTokens().size() == position) {
            token = "";
        } else if (completion.lineTokens().size() == position + 1) {
            token = completion.lineTokens().get(1).raw();
        } else {
            token = null;
        }
        return token;
    }
}
