package pt.fabm.commands;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

import java.util.HashSet;
import java.util.Set;

@Module
public class ActionsSelectorModule {

    @Provides
    @ElementsIntoSet
    Set<AppAction> provides(
            ListServicesCLI listServices,
            DeployServiceCLI deployService,
            LoadServicesCLI loadServices,
            RedeployServiceCLI redeployService,
            ServicesDeployedCLI servicesDeployed,
            UndeployServiceCLI undeployService,
            VerticleByGroup verticleByGroup
    ) {
        HashSet<AppAction> hs = new HashSet<>();
        hs.add(listServices);
        hs.add(deployService);
        hs.add(loadServices);
        hs.add(redeployService);
        hs.add(servicesDeployed);
        hs.add(undeployService);
        hs.add(verticleByGroup);
        return hs;
    }

}
