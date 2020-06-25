package pt.fabm;

import dagger.Component;
import pt.fabm.instances.ServiceTypeCreator;
import pt.fabm.instances.ServiceTypeRegister;

import java.util.function.Consumer;

@Component(modules = AppModule.class)
public interface App {
    ServiceTypeRegister getServiceRegister();
    Consumer<String> run();
}
