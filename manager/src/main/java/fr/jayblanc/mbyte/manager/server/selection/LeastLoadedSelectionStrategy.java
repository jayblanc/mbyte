package fr.jayblanc.mbyte.manager.server.selection;

import fr.jayblanc.mbyte.manager.server.ServerRegistry;
import fr.jayblanc.mbyte.manager.server.entity.Server;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Least-loaded server selection strategy.
 * Selects the server with the fewest stores deployed, ensuring
 * balanced load distribution across servers.
 */
@Singleton
public class LeastLoadedSelectionStrategy implements ServerSelectionStrategy {

    private static final Logger LOGGER = Logger.getLogger(LeastLoadedSelectionStrategy.class.getName());
    private static final String NAME = "least-loaded";

    @Inject
    ServerRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<Server> selectServer(List<Server> availableServers, String owner, String storeName) {
        if (availableServers.isEmpty()) {
            LOGGER.log(Level.WARNING, "No available servers for least-loaded selection");
            return Optional.empty();
        }

        // Find server with lowest store count that still has capacity
        Optional<Server> selected = availableServers.stream()
                .filter(this::hasCapacity)
                .min(Comparator
                        .comparingInt((Server s) -> registry.getStoreCount(s.getId()))
                        .thenComparing(Comparator.comparingInt(Server::getPriority).reversed()));

        if (selected.isPresent()) {
            LOGGER.log(Level.INFO, "Least-loaded selected server: {0} (stores: {1}) for store: {2}",
                    new Object[]{
                            selected.get().getId(),
                            registry.getStoreCount(selected.get().getId()),
                            storeName
                    });
        } else {
            LOGGER.log(Level.WARNING, "All servers are at capacity");
        }

        return selected;
    }

    private boolean hasCapacity(Server server) {
        if (server.getCapacity() == 0) {
            return true; // Unlimited capacity
        }
        int currentCount = registry.getStoreCount(server.getId());
        return currentCount < server.getCapacity();
    }
}
