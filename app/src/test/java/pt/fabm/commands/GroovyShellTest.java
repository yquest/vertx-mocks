package pt.fabm.commands;

import com.fasterxml.jackson.core.JsonPointer;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class GroovyShellTest {
    Logger logger = LoggerFactory.getLogger(GroovyShell.class);
    @Test
    public void testGroovyShell() throws IOException {
        String gre = "/Users/francisco/projs/vertx-services-load-dup" +
                "/groovy-script-example/src/main/resources" +
                "/groovy-router-example.groovy";
        GroovyShell shell = new GroovyShell();
        Script script = shell.parse(new File(gre));
        Binding binding = script.getBinding();
        binding.setVariable("logger",logger);
        Consumer<Closure<?>> consumer = closure -> {
            System.out.println("consuming closure");
        };
        Closure<?> closure = new Closure<Object>(this) {
            @Override
            public Object call(Object... args) {
                System.out.println("consuming "+args[0]);
                return null;
            }
        };
        binding.setVariable("doRequest",closure);
        script.run();
        System.out.println(binding);
    }


}
