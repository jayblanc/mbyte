package fr.jayblanc.mbyte.manager.store;

import fr.jayblanc.mbyte.manager.server.entity.Server;
import fr.jayblanc.mbyte.manager.server.selection.NoServerAvailableException;
import fr.jayblanc.mbyte.manager.server.selection.ServerSelectionManager;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class StoreManager {

    private static final Logger LOGGER = Logger.getLogger(StoreManager.class.getName());

    @Inject
    StoreProviderConfig config;

    @Inject
    Instance<StoreProvider> providers;

    @Inject
    ServerSelectionManager serverSelectionManager;

    public StoreProvider getProvider() throws StoreProviderNotFoundException {
        LOGGER.log(Level.FINE, "Getting store provider for name: {0}", config.provider());
        return providers.stream()
                .filter(p -> p.name().equals(config.provider()))
                .findFirst()
                .orElseThrow(() -> new StoreProviderNotFoundException(
                        "unable to find a provider for name: " + config.provider()));
    }

    /**
     * Select a server for a new store deployment using the configured strategy.
     *
     * @param owner     The owner of the store
     * @param storeName The name of the store
     * @return The selected server
     * @throws NoServerAvailableException if no server is available
     */
    public Server selectServerForNewStore(String owner, String storeName) throws NoServerAvailableException {
        return serverSelectionManager.selectServer(owner, storeName);
    }

    /**
     * Select a server for a new store deployment, optionally using a preferred server.
     *
     * @param owner             The owner of the store
     * @param storeName         The name of the store
     * @param preferredServerId Optional server ID for manual selection
     * @return The selected server
     * @throws NoServerAvailableException if no server is available
     */
    public Server selectServerForNewStore(String owner, String storeName, String preferredServerId)
            throws NoServerAvailableException {
        return serverSelectionManager.selectServer(owner, storeName, preferredServerId);
    }
}
