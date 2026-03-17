package fr.jayblanc.mbyte.manager.server.selection;

import fr.jayblanc.mbyte.manager.server.ServerConfig;
import fr.jayblanc.mbyte.manager.server.ServerRegistry;
import fr.jayblanc.mbyte.manager.server.entity.Server;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager that orchestrates server selection using configured strategy.
 * Supports automatic selection via strategy or manual selection by server ID.
 */
@Singleton
public class ServerSelectionManager {

    private static final Logger LOGGER = Logger.getLogger(ServerSelectionManager.class.getName());

    @Inject
    ServerConfig config;

    @Inject
    ServerRegistry registry;

    @Inject
    Instance<ServerSelectionStrategy> strategies;

    /**
     * Select a server using the configured strategy.
     *
     * @param owner     The owner of the store
     * @param storeName The name of the store
     * @return The selected server
     * @throws NoServerAvailableException if no server is available
     */
    public Server selectServer(String owner, String storeName) throws NoServerAvailableException {
        return selectServer(owner, storeName, null);
    }

    /**
     * Select a server, optionally using a preferred server ID.
     *
     * @param owner             The owner of the store
     * @param storeName         The name of the store
     * @param preferredServerId Optional server ID for manual selection
     * @return The selected server
     * @throws NoServerAvailableException if no server is available
     */
    public Server selectServer(String owner, String storeName, String preferredServerId)
            throws NoServerAvailableException {

        // Manual selection if server ID is provided
        if (preferredServerId != null && !preferredServerId.isEmpty()) {
            LOGGER.log(Level.INFO, "Manual server selection requested: {0}", preferredServerId);
            return selectPreferredServer(preferredServerId);
        }

        // Automatic selection using configured strategy
        return selectUsingStrategy(owner, storeName);
    }

    private Server selectPreferredServer(String serverId) throws NoServerAvailableException {
        Optional<Server> server = registry.getServerById(serverId);

        if (server.isEmpty()) {
            throw new NoServerAvailableException("Server not found: " + serverId);
        }

        Server s = server.get();
        if (!s.isEnabled()) {
            throw new NoServerAvailableException("Server is disabled: " + serverId);
        }

        if (s.getStatus() != Server.ServerStatus.ONLINE) {
            throw new NoServerAvailableException("Server is not online: " + serverId +
                    " (status: " + s.getStatus() + ")");
        }

        // Check capacity
        if (s.getCapacity() > 0 && registry.getStoreCount(serverId) >= s.getCapacity()) {
            throw new NoServerAvailableException("Server is at capacity: " + serverId);
        }

        LOGGER.log(Level.INFO, "Manual server selection successful: {0}", serverId);
        return s;
    }

    private Server selectUsingStrategy(String owner, String storeName) throws NoServerAvailableException {
        String strategyName = config.selectionStrategy();
        LOGGER.log(Level.INFO, "Using selection strategy: {0}", strategyName);

        ServerSelectionStrategy strategy = strategies.stream()
                .filter(s -> s.name().equals(strategyName))
                .findFirst()
                .orElseThrow(() -> new NoServerAvailableException(
                        "Unknown selection strategy: " + strategyName));

        return strategy.selectServer(registry.getEnabledServers(), owner, storeName)
                .orElseThrow(() -> new NoServerAvailableException(
                        "No available server for store deployment. All servers may be offline or at capacity."));
    }
}
