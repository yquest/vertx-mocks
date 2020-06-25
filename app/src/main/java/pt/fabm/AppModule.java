package pt.fabm;

import dagger.Module;
import dagger.Provides;
import pt.fabm.instances.ServiceTypeRegister;

import java.util.function.Consumer;

@Module
public class AppModule {
    private ServerLauncher launcher;

    public AppModule() {
        launcher = new ServerLauncher();
        launcher.getServiceRegister().register(new GroovyDslServiceType());
    }

    @Provides
    Consumer<String> providesApp() {
        return confPath -> launcher
                .confPath(confPath)
                .run();
    }

    @Provides
    ServiceTypeRegister providesServiceTypeCreator() {
        return launcher.getServiceRegister();
    }
}
