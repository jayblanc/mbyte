package fr.jayblanc.mbyte.manager.store;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class StoreManager {

    private static final  Logger LOGGER = Logger.getLogger(StoreManager.class.getName());

    @Inject StoreProviderConfig config;
    @Inject Instance<StoreProvider> providers;

    public StoreProvider getProvider() throws StoreProviderNotFoundException {
        LOGGER.log(Level.FINE, "Getting store provider for name: " + config.provider());
        return providers.stream().filter(p -> p.name().equals(config.provider())).findFirst().orElseThrow(() -> new StoreProviderNotFoundException("unable to find a provider for name: " + config.provider()));
    }

}
