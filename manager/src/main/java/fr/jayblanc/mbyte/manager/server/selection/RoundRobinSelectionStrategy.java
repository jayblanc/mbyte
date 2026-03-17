package fr.jayblanc.mbyte.manager.server.selection;

import fr.jayblanc.mbyte.manager.server.ServerRegistry;
import fr.jayblanc.mbyte.manager.server.entity.Server;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Round-robin server selection strategy.
 * Distributes stores evenly across all available servers by selecting
 * each server in turn.
 */
@Singleton
public class RoundRobinSelectionStrategy implements ServerSelectionStrategy {

    private static final Logger LOGGER = Logger.getLogger(RoundRobinSelectionStrategy.class.getName());
    private static final String NAME = "round-robin";

    private final AtomicInteger counter = new AtomicInteger(0);

    @Inject
    ServerRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<Server> selectServer(List<Server> availableServers, String owner, String storeName) {
        if (availableServers.isEmpty()) {
            LOGGER.log(Level.WARNING, "No available servers for round-robin selection");
            return Optional.empty();
        }

        // Filter servers that have capacity
        List<Server> serversWithCapacity = availableServers.stream()
                .filter(this::hasCapacity)
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority())) // Higher priority first
                .toList();

        if (serversWithCapacity.isEmpty()) {
            LOGGER.log(Level.WARNING, "All servers are at capacity");
            return Optional.empty();
        }

        int index = Math.abs(counter.getAndIncrement() % serversWithCapacity.size());
        Server selected = serversWithCapacity.get(index);

        LOGGER.log(Level.INFO, "Round-robin selected server: {0} for store: {1}",
                new Object[]{selected.getId(), storeName});

        return Optional.of(selected);
    }

    private boolean hasCapacity(Server server) {
        if (server.getCapacity() == 0) {
            return true; // Unlimited capacity
        }
        int currentCount = registry.getStoreCount(server.getId());
        return currentCount < server.getCapacity();
    }
}
