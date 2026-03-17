package fr.jayblanc.mbyte.manager.server.selection;

import fr.jayblanc.mbyte.manager.server.entity.Server;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for selecting a server to host a new store.
 * Implementations can use different algorithms (round-robin, least-loaded, etc.)
 */
public interface ServerSelectionStrategy {

    /**
     * Get the name of this strategy (used for configuration matching)
     */
    String name();

    /**
     * Select a server from the available servers for deploying a new store.
     *
     * @param availableServers List of servers that are enabled and online
     * @param owner            The owner of the store being created
     * @param storeName        The name of the store being created
     * @return The selected server, or empty if no suitable server is available
     */
    Optional<Server> selectServer(List<Server> availableServers, String owner, String storeName);
}
