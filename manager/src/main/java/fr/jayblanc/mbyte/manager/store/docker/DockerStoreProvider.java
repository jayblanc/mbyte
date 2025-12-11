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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
public class DockerStoreProvider implements StoreProvider {

    private static final Logger LOGGER = Logger.getLogger(DockerStoreProvider.class.getName());
    private static final String NAME = "docker";

    private static final String INSTANCE_NAME = "mbyte.";
    private static final String NETWORK_NAME = "mbyte.net";
    private static final String STORES_DATA_PATH_SEGMENT = "data";
    private static final String STORES_DB_PATH_SEGMENT = "db";
    private static final String VOLUME_SUFFIX = ".volume";
    private static final String CONTAINER_SUFFIX = ".cont";
    private static final String DATA_SUFFIX = ".data";
    private static final String STORE_SUFFIX = ".store";
    private static final String DB_SUFFIX = ".db";

    private DockerClient client;

    @Inject StoreProviderConfig storeConfig;
    @Inject DockerStoreProviderConfig config;

    @PostConstruct
    private void init() {
        if ( !storeConfig.provider().equals(NAME) ) {
            LOGGER.log(Level.INFO, "DockerStoreProvider not activated (provider is " + storeConfig.provider() + ")");
            return;
        }
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
        client = DockerClientImpl.getInstance(clientConfig, httpClient);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<String> listAllStores() {
        LOGGER.log(Level.INFO, "Listing store apps");
        return client.listContainersCmd().exec().stream().map(container -> Arrays.stream(container.getNames()).collect(Collectors.joining()) + " / " + container.getImage()).collect(Collectors.toList());
    }

    @Override
    public String createStore(String id, String owner, String name) {
        LOGGER.log(Level.INFO, "Starting new store creation...");
        StringBuilder creationLog = new StringBuilder();

        // Step 1: load network 'mbyte.net'
        Optional<Network> network = client.listNetworksCmd().withNameFilter(NETWORK_NAME).exec().stream().findFirst();
        if (network.isEmpty()) {
            LOGGER.log(Level.SEVERE, NETWORK_NAME + " network not found, cannot create store app");
            creationLog.append("[Step 1/9] -FAILED- ").append(NETWORK_NAME).append(" network not found, cannot create store app");
            return creationLog.toString();
        }
        LOGGER.log(Level.INFO, "Found existing network, name: " + network.get().getName() + ", id:" + network.get().getId());
        creationLog.append("[Step 1/9] -COMPLETED- ").append("Found existing network, name:").append(network.get().getName()).append(", id:").append(network.get().getId()).append("\n");

        // Step 2: create db volume 'mbyte.UUID.db.volume'
        String dbVolumeName = INSTANCE_NAME.concat(id).concat(DB_SUFFIX).concat(VOLUME_SUFFIX);
        Path dbLocalVolumePath =  Paths.get(config.workdir().local(), id, STORES_DB_PATH_SEGMENT);
        Path dbHostVolumePath =  Paths.get(config.workdir().host(), id, STORES_DB_PATH_SEGMENT);
        try {
            Files.createDirectories(dbLocalVolumePath);
            LOGGER.log(Level.INFO, "Created directories for db volume: " + dbLocalVolumePath);
            creationLog.append("[Step 2/9] -PROGRESS- ").append("Created directories for db volume: ").append(dbLocalVolumePath).append("\n");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create directories for store db volume: " + dbLocalVolumePath, e);
            creationLog.append("[Step 2/9] -FAILED- ").append("Failed to create directories for store db volume: ").append(dbLocalVolumePath).append("\n");
            return creationLog.toString();
        }
        Optional<InspectVolumeResponse> dbVolume = client.listVolumesCmd().exec().getVolumes().stream()
            .filter(v -> dbVolumeName.equals(v.getName()))
            .findFirst();
        if (dbVolume.isEmpty()) {
            CreateVolumeResponse response = client.createVolumeCmd()
                .withName(dbVolumeName)
                .withDriver("local")
                .withDriverOpts(Map.of("type", "none", "o", "bind","device", dbHostVolumePath.toString()))
                .exec();
            LOGGER.log(Level.INFO, "Database volume created: " + response.getName());
            creationLog.append("[Step 2/9] -COMPLETED- ").append("Database volume created: ").append(response.getName()).append("\n");
        } else {
            LOGGER.log(Level.INFO, "Found existing database volume: " + dbVolume.get().getName());
            creationLog.append("[Step 2/9] -COMPLETED- ").append("Found existing database volume: ").append(dbVolume.get().getName()).append("\n");
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
                .withBinds(new Bind(dbVolumeName, new Volume("/var/lib/postgresql/data"))))
            .exec();
        LOGGER.log(Level.INFO, "Database container created for store: " + dbContainer.getId());
        creationLog.append("[Step 3/9] -COMPLETED- ").append("Database container created for store: ").append(dbContainer.getId()).append("\n");

        // Step 4: connect db container to network
        client.connectToNetworkCmd().withContainerId(dbContainer.getId()).withNetworkId(network.get().getId()).exec();
        LOGGER.log(Level.INFO, "Database container connected to " + NETWORK_NAME);
        creationLog.append("[Step 4/9] -COMPLETED- ").append("Database container connected to network: ").append(NETWORK_NAME).append("\n");

        // Step 5: start db container
        client.startContainerCmd(dbContainer.getId()).exec();
        LOGGER.log(Level.INFO, "Database container started for store: " + dbContainer.getId());
        creationLog.append("[Step 5/9] -COMPLETED- ").append("Database container started for store: ").append(dbContainer.getId()).append("\n");

        // Step 6: create data volume 'mbyte.UUID.data.volume'
        String dataVolumeName = INSTANCE_NAME.concat(id).concat(DATA_SUFFIX).concat(VOLUME_SUFFIX);
        Path dataLocalVolumePath =  Paths.get(config.workdir().local(), id, STORES_DATA_PATH_SEGMENT);
        Path dataHostVolumePath =  Paths.get(config.workdir().host(), id, STORES_DATA_PATH_SEGMENT);
        try {
            Files.createDirectories(dataLocalVolumePath);
            LOGGER.log(Level.INFO, "Created directories for data volume: " + dataLocalVolumePath);
            creationLog.append("[Step 6/9] -PROGRESS- ").append("Created directories for data volume: ").append(dataLocalVolumePath).append("\n");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create directories for store data volume: " + dataLocalVolumePath, e);
            creationLog.append("[Step 6/9] -FAILED- ").append("Failed to create directories for store data volume: ").append(dataLocalVolumePath).append("\n");
            return creationLog.toString();
        }
        Optional<InspectVolumeResponse> dataVolume = client.listVolumesCmd().exec().getVolumes().stream()
                .filter(v -> dataVolumeName.equals(v.getName()))
                .findFirst();
        if (dataVolume.isEmpty()) {
            CreateVolumeResponse response = client.createVolumeCmd()
                    .withName(dataVolumeName)
                    .withDriver("local")
                    .withDriverOpts(Map.of("type", "none", "o", "bind","device", dataHostVolumePath.toString()))
                    .exec();
            LOGGER.log(Level.INFO, "Data volume created: " + response.getName());
            creationLog.append("[Step 6/9] -COMPLETED- ").append("Data volume created: ").append(response.getName()).append("\n");
        } else {
            LOGGER.log(Level.INFO, "Found existing data volume: " + dataVolume.get().getName());
            creationLog.append("[Step 6/9] -COMPLETED- ").append("Found existing data volume: ").append(dataVolume.get().getName()).append("\n");
        }

        // Step 7: create store container 'mbyte.UUID.store.cont'
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
                        "traefik.http.services." + id + "-http.loadbalancer.server.port","8080"
                ))
                .withHostConfig(HostConfig.newHostConfig()
                        .withBinds(new Bind(dataVolumeName, new Volume("/home/jboss"))))
                .exec();
        LOGGER.log(Level.INFO, "Store container created: " + storeContainer.getId());
        creationLog.append("[Step 7/9] -COMPLETED- ").append("Store container created: ").append(storeContainer.getId()).append("\n");

        // Step 8: connect store container to the network
        client.connectToNetworkCmd().withContainerId(storeContainer.getId()).withNetworkId(network.get().getId()).exec();
        LOGGER.log(Level.INFO, "Store container connected to " + NETWORK_NAME);
        creationLog.append("[Step 8/9] -COMPLETED- ").append("Store container connected to network: ").append(NETWORK_NAME).append("\n");

        // Step 9: start store container
        client.startContainerCmd(storeContainer.getId()).exec();
        LOGGER.log(Level.INFO, "Store container started for store: " + storeContainer.getId());
        creationLog.append("[Step 9/9] -COMPLETED- ").append("Store container started for id: ").append(storeContainer.getId()).append("\n");

        return id;
    }

    @Override
    public String destroyStore(String id) throws StoreProviderException {
        LOGGER.log(Level.INFO, "Destroying store app");
        return "";
    }
}
