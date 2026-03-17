package fr.jayblanc.mbyte.manager.server;

import com.github.dockerjava.api.DockerClient;
import fr.jayblanc.mbyte.manager.server.entity.Server;

import java.util.List;
import java.util.Optional;

/**
 * Registry for managing Docker servers available for store deployment.
 * Provides access to server information and Docker clients.
 */
public interface ServerRegistry {

    /**
     * Get all configured servers
     */
    List<Server> getAllServers();

    /**
     * Get servers that are enabled and online
     */
    List<Server> getEnabledServers();

    /**
     * Get a server by its ID
     */
    Optional<Server> getServerById(String serverId);

    /**
     * Check the health status of a server
     */
    Server.ServerStatus checkServerHealth(String serverId);

    /**
     * Get the number of stores deployed on a server
     */
    int getStoreCount(String serverId);

    /**
     * Refresh the health status of all servers
     */
    void refreshServerStatuses();

    /**
     * Get the Docker client for a specific server
     */
    DockerClient getDockerClient(String serverId);
}
