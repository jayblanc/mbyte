package fr.jayblanc.mbyte.manager.server;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import fr.jayblanc.mbyte.manager.server.entity.Server;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of ServerRegistry that manages Docker server connections.
 * Loads server configurations from application.properties and maintains
 * Docker client connections for each server.
 */
@Singleton
@Startup
public class ServerRegistryBean implements ServerRegistry {

    private static final Logger LOGGER = Logger.getLogger(ServerRegistryBean.class.getName());

    @Inject
    ServerConfig config;

    @Inject
    EntityManager em;

    private final Map<String, Server> servers = new ConcurrentHashMap<>();
    private final Map<String, DockerClient> dockerClients = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "Initializing Server Registry for multi-server deployment");
        loadServersFromConfig();
        refreshServerStatuses();
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.log(Level.INFO, "Cleaning up Server Registry - closing Docker clients");
        dockerClients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing Docker client", e);
            }
        });
    }

    private void loadServersFromConfig() {
        if (config.servers() == null || config.servers().isEmpty()) {
            LOGGER.log(Level.WARNING, "No servers configured in manager.servers.servers");
            return;
        }

        config.servers().forEach((id, def) -> {
            Server server = new Server();
            server.setId(id);
            server.setName(def.name());
            server.setDockerHost(def.dockerHost());
            server.setWorkdirHost(def.workdirHost());
            server.setWorkdirLocal(def.workdirLocal());
            server.setTraefikNetwork(def.traefikNetwork());
            server.setEnabled(def.enabled());
            server.setCapacity(def.capacity());
            server.setPriority(def.priority());
            server.setStatus(Server.ServerStatus.UNKNOWN);
            servers.put(id, server);

            initDockerClient(server);
        });

        LOGGER.log(Level.INFO, "Loaded {0} servers from configuration", servers.size());
    }

    private void initDockerClient(Server server) {
        try {
            DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(server.getDockerHost())
                    .build();

            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(clientConfig.getDockerHost())
                    .sslConfig(clientConfig.getSSLConfig())
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45))
                    .build();

            DockerClient client = DockerClientImpl.getInstance(clientConfig, httpClient);
            dockerClients.put(server.getId(), client);

            LOGGER.log(Level.INFO, "Initialized Docker client for server: {0} ({1})",
                    new Object[]{server.getId(), server.getDockerHost()});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to initialize Docker client for server: " + server.getId(), e);
            server.setStatus(Server.ServerStatus.OFFLINE);
        }
    }

    @Override
    public DockerClient getDockerClient(String serverId) {
        return dockerClients.get(serverId);
    }

    @Override
    public List<Server> getAllServers() {
        return new ArrayList<>(servers.values());
    }

    @Override
    public List<Server> getEnabledServers() {
        return servers.values().stream()
                .filter(Server::isEnabled)
                .filter(s -> s.getStatus() == Server.ServerStatus.ONLINE)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Server> getServerById(String serverId) {
        return Optional.ofNullable(servers.get(serverId));
    }

    @Override
    public Server.ServerStatus checkServerHealth(String serverId) {
        DockerClient client = dockerClients.get(serverId);
        if (client == null) {
            return Server.ServerStatus.OFFLINE;
        }
        try {
            client.listNetworksCmd().exec();
            return Server.ServerStatus.ONLINE;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Health check failed for server: " + serverId, e);
            return Server.ServerStatus.OFFLINE;
        }
    }

    @Override
    public int getStoreCount(String serverId) {
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(s) FROM Store s WHERE s.serverId = :serverId", Long.class)
                    .setParameter("serverId", serverId)
                    .getSingleResult();
            return count.intValue();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error counting stores for server: " + serverId, e);
            return 0;
        }
    }

    @Override
    @Scheduled(every = "60s")
    public void refreshServerStatuses() {
        LOGGER.log(Level.FINE, "Refreshing server statuses");
        servers.values().forEach(server -> {
            Server.ServerStatus status = checkServerHealth(server.getId());
            server.setStatus(status);
            LOGGER.log(Level.FINE, "Server {0} status: {1}",
                    new Object[]{server.getId(), status});
        });
    }
}
