package fr.jayblanc.mbyte.manager.store.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import fr.jayblanc.mbyte.manager.server.ServerRegistry;
import fr.jayblanc.mbyte.manager.server.entity.Server;
import fr.jayblanc.mbyte.manager.store.StoreProvider;
import fr.jayblanc.mbyte.manager.store.StoreProviderConfig;
import fr.jayblanc.mbyte.manager.store.StoreProviderException;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Docker-based store provider with multi-server support.
 * Can deploy stores to local or remote Docker daemons.
 */
@Singleton
public class DockerStoreProvider implements StoreProvider {

    private static final Logger LOGGER = Logger.getLogger(DockerStoreProvider.class.getName());
    private static final String NAME = "docker";

    private static final String INSTANCE_NAME = "mbyte.";
    private static final String DEFAULT_NETWORK_NAME = "mbyte.net";
    private static final String STORES_DATA_PATH_SEGMENT = "data";
    private static final String STORES_DB_PATH_SEGMENT = "db";
    private static final String VOLUME_SUFFIX = ".volume";
    private static final String CONTAINER_SUFFIX = ".cont";
    private static final String DATA_SUFFIX = ".data";
    private static final String STORE_SUFFIX = ".store";
    private static final String DB_SUFFIX = ".db";

    // Legacy client for backwards compatibility (single server mode)
    private DockerClient legacyClient;

    @Inject
    StoreProviderConfig storeConfig;

    @Inject
    DockerStoreProviderConfig config;

    @Inject
    ServerRegistry serverRegistry;

    @PostConstruct
    private void init() {
        if (!storeConfig.provider().equals(NAME)) {
            LOGGER.log(Level.INFO, "DockerStoreProvider not activated (provider is " + storeConfig.provider() + ")");
            return;
        }
        // Initialize legacy client for backwards compatibility
        DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(config.server())
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        legacyClient = DockerClientImpl.getInstance(clientConfig, httpClient);
        LOGGER.log(Level.INFO, "DockerStoreProvider initialized with multi-server support");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<String> listAllStores() throws StoreProviderException {
        LOGGER.log(Level.INFO, "Listing store apps from all servers");
        List<String> allStores = new ArrayList<>();

        // List from all enabled servers
        for (Server server : serverRegistry.getEnabledServers()) {
            try {
                allStores.addAll(listStores(server));
            } catch (StoreProviderException e) {
                LOGGER.log(Level.WARNING, "Failed to list stores from server: " + server.getId(), e);
            }
        }

        // Fallback to legacy client if no servers configured
        if (serverRegistry.getAllServers().isEmpty() && legacyClient != null) {
            return legacyClient.listContainersCmd().exec().stream()
                    .map(container -> Arrays.stream(container.getNames()).collect(Collectors.joining()) + " / " + container.getImage())
                    .collect(Collectors.toList());
        }

        return allStores;
    }

    @Override
    public List<String> listStores(Server server) throws StoreProviderException {
        DockerClient client = serverRegistry.getDockerClient(server.getId());
        if (client == null) {
            throw new StoreProviderException("No Docker client available for server: " + server.getId());
        }
        return client.listContainersCmd().exec().stream()
                .map(container -> "[" + server.getId() + "] " +
                        Arrays.stream(container.getNames()).collect(Collectors.joining()) +
                        " / " + container.getImage())
                .collect(Collectors.toList());
    }

    @Override
    public String createStore(String id, String owner, String name) throws StoreProviderException {
        // Backwards compatible: use first enabled server or legacy client
        List<Server> enabledServers = serverRegistry.getEnabledServers();
        if (!enabledServers.isEmpty()) {
            return createStore(id, owner, name, enabledServers.get(0));
        }

        // Fallback to legacy single-server mode
        LOGGER.log(Level.INFO, "Using legacy single-server mode for store creation");
        return createStoreWithClient(id, owner, name, legacyClient, config.workdir().host(),
                config.workdir().local(), DEFAULT_NETWORK_NAME, false);
    }

    @Override
    public String createStore(String id, String owner, String name, Server server) throws StoreProviderException {
        LOGGER.log(Level.INFO, "Creating store on server: {0}", server.getId());

        DockerClient client = serverRegistry.getDockerClient(server.getId());
        if (client == null) {
            throw new StoreProviderException("No Docker client available for server: " + server.getId());
        }

        String workdirHost = server.getWorkdirHost();
        String workdirLocal = server.getWorkdirLocal();
        if (workdirLocal == null || workdirLocal.isEmpty()) {
            workdirLocal = workdirHost; // Use host path if local not specified
        }
        String networkName = server.getTraefikNetwork();
        boolean isRemoteDaemon = server.getDockerHost().startsWith("tcp://");

        return createStoreWithClient(id, owner, name, client, workdirHost, workdirLocal, networkName, isRemoteDaemon);
    }

    private String createStoreWithClient(String id, String owner, String name, DockerClient client,
                                         String workdirHost, String workdirLocal, String networkName,
                                         boolean isRemoteDaemon)
            throws StoreProviderException {

        LOGGER.log(Level.INFO, "Starting new store creation... (remoteDaemon={0})", isRemoteDaemon);
        StringBuilder creationLog = new StringBuilder();

        // Step 1: load network (create it on remote daemons if missing)
        Optional<Network> network = client.listNetworksCmd().withNameFilter(networkName).exec().stream().findFirst();
        if (network.isEmpty()) {
            if (isRemoteDaemon) {
                LOGGER.log(Level.INFO, "Network {0} not found on remote daemon, creating it", networkName);
                client.createNetworkCmd().withName(networkName).withDriver("bridge").exec();
                network = client.listNetworksCmd().withNameFilter(networkName).exec().stream().findFirst();
            }
            if (network.isEmpty()) {
                LOGGER.log(Level.SEVERE, networkName + " network not found, cannot create store app");
                creationLog.append("[Step 1/7] -FAILED- ").append(networkName).append(" network not found, cannot create store app");
                throw new StoreProviderException(creationLog.toString());
            }
        }
        LOGGER.log(Level.INFO, "Found existing network, name: " + network.get().getName() + ", id:" + network.get().getId());
        creationLog.append("[Step 1/7] -COMPLETED- ").append("Found existing network, name:").append(network.get().getName()).append(", id:").append(network.get().getId()).append("\n");

        // Step 2: create db volume 'mbyte.UUID.db.volume'
        String dbVolumeName = INSTANCE_NAME.concat(id).concat(DB_SUFFIX).concat(VOLUME_SUFFIX);
        Path dbHostVolumePath = Paths.get(workdirHost, id, STORES_DB_PATH_SEGMENT);
        if (!isRemoteDaemon) {
            Path dbLocalVolumePath = Paths.get(workdirLocal, id, STORES_DB_PATH_SEGMENT);
            try {
                Files.createDirectories(dbLocalVolumePath);
                LOGGER.log(Level.INFO, "Created directories for db volume: " + dbLocalVolumePath);
                creationLog.append("[Step 2/7] -PROGRESS- ").append("Created directories for db volume: ").append(dbLocalVolumePath).append("\n");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to create directories for store db volume: " + dbLocalVolumePath, e);
                creationLog.append("[Step 2/7] -FAILED- ").append("Failed to create directories for store db volume: ").append(dbLocalVolumePath).append("\n");
                throw new StoreProviderException(creationLog.toString());
            }
        }
        Optional<InspectVolumeResponse> dbVolume = client.listVolumesCmd().exec().getVolumes().stream()
                .filter(v -> dbVolumeName.equals(v.getName()))
                .findFirst();
        if (dbVolume.isEmpty()) {
            CreateVolumeResponse response;
            if (isRemoteDaemon) {
                // Remote daemon: use native Docker volume (no bind mount)
                response = client.createVolumeCmd()
                        .withName(dbVolumeName)
                        .exec();
            } else {
                // Local daemon: use bind mount to host path
                response = client.createVolumeCmd()
                        .withName(dbVolumeName)
                        .withDriver("local")
                        .withDriverOpts(Map.of("type", "none", "o", "bind", "device", dbHostVolumePath.toString()))
                        .exec();
            }
            LOGGER.log(Level.INFO, "Database volume created: " + response.getName());
            creationLog.append("[Step 2/7] -COMPLETED- ").append("Database volume created: ").append(response.getName()).append("\n");
        } else {
            LOGGER.log(Level.INFO, "Found existing database volume: " + dbVolume.get().getName());
            creationLog.append("[Step 2/7] -COMPLETED- ").append("Found existing database volume: ").append(dbVolume.get().getName()).append("\n");
        }

        // Step 3: create db container 'mbyte.UUID.db.cont'
        String dbContainerName = INSTANCE_NAME.concat(id).concat(DB_SUFFIX).concat(CONTAINER_SUFFIX);
        String dbContainerPassword = "Pp@asSw#".concat(id).concat("#W0orRdD!");
        CreateContainerResponse dbContainer = client.createContainerCmd("postgres:latest")
                .withName(dbContainerName)
                .withHostName(dbContainerName)
                .withEnv(
                        "POSTGRES_USER=" + id,
                        "POSTGRES_PASSWORD=" + dbContainerPassword,
                        "POSTGRES_DB=store"
                )
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(networkName)
                        .withBinds(new Bind(dbVolumeName, new Volume("/var/lib/postgresql/data"))))
                .exec();
        LOGGER.log(Level.INFO, "Database container created for store: " + dbContainer.getId());
        creationLog.append("[Step 3/7] -COMPLETED- ").append("Database container created for store: ").append(dbContainer.getId()).append("\n");

        // Step 4: start db container
        client.startContainerCmd(dbContainer.getId()).exec();
        LOGGER.log(Level.INFO, "Database container started for store: " + dbContainer.getId());
        creationLog.append("[Step 4/7] -COMPLETED- ").append("Database container started for store: ").append(dbContainer.getId()).append("\n");

        // Step 5: create data volume 'mbyte.UUID.data.volume'
        String dataVolumeName = INSTANCE_NAME.concat(id).concat(DATA_SUFFIX).concat(VOLUME_SUFFIX);
        Path dataHostVolumePath = Paths.get(workdirHost, id, STORES_DATA_PATH_SEGMENT);
        if (!isRemoteDaemon) {
            Path dataLocalVolumePath = Paths.get(workdirLocal, id, STORES_DATA_PATH_SEGMENT);
            try {
                Files.createDirectories(dataLocalVolumePath);
                LOGGER.log(Level.INFO, "Created directories for data volume: " + dataLocalVolumePath);
                creationLog.append("[Step 5/7] -PROGRESS- ").append("Created directories for data volume: ").append(dataLocalVolumePath).append("\n");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to create directories for store data volume: " + dataLocalVolumePath, e);
                creationLog.append("[Step 5/7] -FAILED- ").append("Failed to create directories for store data volume: ").append(dataLocalVolumePath).append("\n");
                throw new StoreProviderException(creationLog.toString());
            }
        }
        Optional<InspectVolumeResponse> dataVolume = client.listVolumesCmd().exec().getVolumes().stream()
                .filter(v -> dataVolumeName.equals(v.getName()))
                .findFirst();
        if (dataVolume.isEmpty()) {
            CreateVolumeResponse response;
            if (isRemoteDaemon) {
                // Remote daemon: use native Docker volume (no bind mount)
                response = client.createVolumeCmd()
                        .withName(dataVolumeName)
                        .exec();
            } else {
                // Local daemon: use bind mount to host path
                response = client.createVolumeCmd()
                        .withName(dataVolumeName)
                        .withDriver("local")
                        .withDriverOpts(Map.of("type", "none", "o", "bind", "device", dataHostVolumePath.toString()))
                        .exec();
            }
            LOGGER.log(Level.INFO, "Data volume created: " + response.getName());
            creationLog.append("[Step 5/7] -COMPLETED- ").append("Data volume created: ").append(response.getName()).append("\n");
        } else {
            LOGGER.log(Level.INFO, "Found existing data volume: " + dataVolume.get().getName());
            creationLog.append("[Step 5/7] -COMPLETED- ").append("Found existing data volume: ").append(dataVolume.get().getName()).append("\n");
        }

        // Step 6: create store container 'mbyte.UUID.store.cont'
        String storeContainerName = INSTANCE_NAME.concat(id).concat(STORE_SUFFIX).concat(CONTAINER_SUFFIX);
        CreateContainerResponse storeContainer = client.createContainerCmd(config.image())
                .withName(storeContainerName)
                .withHostName(storeContainerName)
                .withEnv(
                        "QUARKUS_HTTP_PORT=8080",
                        "STORE.ROOT=/home/jboss",
                        "STORE.AUTH.OWNER=" + owner,
                        "STORE.TOPOLOGY.HOST=consul",
                        "STORE.TOPOLOGY.PORT=8500",
                        "STORE.TOPOLOGY.SERVICE.HOST=" + name + ".stores.mbyte.fr",
                        "QUARKUS.DATASOURCE.USERNAME=" + id,
                        "QUARKUS.DATASOURCE.PASSWORD=" + dbContainerPassword,
                        "QUARKUS.DATASOURCE.JDBC.URL=jdbc:postgresql://" + dbContainerName + ":5432/store"
                )
                .withLabels(Map.of(
                        "traefik.enable", "true",
                        "traefik.docker.network", "mbyte",
                        "traefik.http.routers." + id + ".rule", "Host(`" + name + ".stores.mbyte.fr`)",
                        "traefik.http.routers." + id + ".entrypoints", "http",
                        "traefik.http.routers." + id + ".service", id + "-http",
                        "traefik.http.services." + id + "-http.loadbalancer.server.port", "8080"
                ))
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(networkName)
                        .withBinds(new Bind(dataVolumeName, new Volume("/home/jboss"))))
                .exec();
        LOGGER.log(Level.INFO, "Store container created: " + storeContainer.getId());
        creationLog.append("[Step 6/7] -COMPLETED- ").append("Store container created: ").append(storeContainer.getId()).append("\n");

        // Step 7: start store container
        client.startContainerCmd(storeContainer.getId()).exec();
        LOGGER.log(Level.INFO, "Store container started for store: " + storeContainer.getId());
        creationLog.append("[Step 7/7] -COMPLETED- ").append("Store container started for id: ").append(storeContainer.getId()).append("\n");

        return id;
    }

    @Override
    public String destroyStore(String id) throws StoreProviderException {
        LOGGER.log(Level.INFO, "Destroying store: {0} (searching across all servers)", id);

        // Try each enabled server
        for (Server server : serverRegistry.getEnabledServers()) {
            try {
                return destroyStore(id, server);
            } catch (StoreProviderException e) {
                LOGGER.log(Level.FINE, "Store not found on server: " + server.getId());
            }
        }

        // Fallback to legacy client
        if (legacyClient != null) {
            return destroyStoreWithClient(id, legacyClient);
        }

        throw new StoreProviderException("Store not found on any server: " + id);
    }

    @Override
    public String destroyStore(String id, Server server) throws StoreProviderException {
        LOGGER.log(Level.INFO, "Destroying store {0} on server: {1}", new Object[]{id, server.getId()});

        DockerClient client = serverRegistry.getDockerClient(server.getId());
        if (client == null) {
            throw new StoreProviderException("No Docker client available for server: " + server.getId());
        }

        return destroyStoreWithClient(id, client);
    }

    private String destroyStoreWithClient(String id, DockerClient client) throws StoreProviderException {
        StringBuilder destroyLog = new StringBuilder();

        // Stop and remove store container
        String storeContainerName = INSTANCE_NAME.concat(id).concat(STORE_SUFFIX).concat(CONTAINER_SUFFIX);
        try {
            client.stopContainerCmd(storeContainerName).exec();
            client.removeContainerCmd(storeContainerName).exec();
            LOGGER.log(Level.INFO, "Store container removed: " + storeContainerName);
            destroyLog.append("Store container removed: ").append(storeContainerName).append("\n");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error removing store container: " + storeContainerName, e);
            destroyLog.append("Warning: Could not remove store container: ").append(storeContainerName).append("\n");
        }

        // Stop and remove db container
        String dbContainerName = INSTANCE_NAME.concat(id).concat(DB_SUFFIX).concat(CONTAINER_SUFFIX);
        try {
            client.stopContainerCmd(dbContainerName).exec();
            client.removeContainerCmd(dbContainerName).exec();
            LOGGER.log(Level.INFO, "Database container removed: " + dbContainerName);
            destroyLog.append("Database container removed: ").append(dbContainerName).append("\n");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error removing database container: " + dbContainerName, e);
            destroyLog.append("Warning: Could not remove database container: ").append(dbContainerName).append("\n");
        }

        // Remove volumes
        String dataVolumeName = INSTANCE_NAME.concat(id).concat(DATA_SUFFIX).concat(VOLUME_SUFFIX);
        String dbVolumeName = INSTANCE_NAME.concat(id).concat(DB_SUFFIX).concat(VOLUME_SUFFIX);
        try {
            client.removeVolumeCmd(dataVolumeName).exec();
            LOGGER.log(Level.INFO, "Data volume removed: " + dataVolumeName);
            destroyLog.append("Data volume removed: ").append(dataVolumeName).append("\n");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error removing data volume: " + dataVolumeName, e);
        }
        try {
            client.removeVolumeCmd(dbVolumeName).exec();
            LOGGER.log(Level.INFO, "Database volume removed: " + dbVolumeName);
            destroyLog.append("Database volume removed: ").append(dbVolumeName).append("\n");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error removing database volume: " + dbVolumeName, e);
        }

        return destroyLog.toString();
    }
}
