package pt.fabm;


import pt.fabm.instances.Context;
import pt.fabm.instances.DaggerContext;
import pt.fabm.instances.ServiceTypeRegister;

public class ServerLauncher {

    private final Context context;
    private String confPath;

    public ServerLauncher() {
        context = DaggerContext.create();
    }

    public ServerLauncher confPath(String confPath) {
        this.confPath = confPath;
        return this;
    }

    public ServiceTypeRegister getServiceRegister() {
        return context.getServiceRegister();
    }

    public void run() {
        context.getApp().init(confPath);
    }
}