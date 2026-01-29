/*
 * Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.jayblanc.mbyte.manager.core.runtime.task.store;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import fr.jayblanc.mbyte.manager.process.Task;
import fr.jayblanc.mbyte.manager.process.TaskException;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionScoped;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@TransactionScoped
public class CreateDockerStoreTask extends Task {

    public static final String TASK_NAME = "StartDockerStore";
    public static final String NETWORK_NAME = "STORE_NETWORK_NAME";
    public static final String STORE_IMAGE_NAME = "STORE_IMAGE_NAME";
    public static final String STORE_NAME = "STORE_NAME";
    public static final String STORE_VOLUME_NAME = "STORE_VOLUME_NAME";
    public static final String STORE_CONTAINER_NAME = "STORE_CONTAINER_NAME";
    public static final String STORE_OWNER = "STORE_OWNER";
    public static final String STORE_FQDN = "STORE_FQDN";
    public static final String STORE_TOPOLOGY_ENABLED = "STORE_TOPOLOGY_ENABLED";
    public static final String STORE_DB_CONTAINER_NAME = "STORE_DB_CONTAINER_NAME";
    public static final String STORE_DB_NAME = "STORE_DB_NAME";
    public static final String STORE_DB_USER = "STORE_DB_USER";
    public static final String STORE_DB_PASSWORD = "STORE_DB_PASSWORD";

    @Inject DockerClient client;

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void execute() throws TaskException {
        String networkName = getMandatoryContextValue(NETWORK_NAME);
        String imageName = getMandatoryContextValue(STORE_IMAGE_NAME);
        String volumeName = getMandatoryContextValue(STORE_VOLUME_NAME);
        String containerName = getMandatoryContextValue(STORE_CONTAINER_NAME);
        String storeName = getMandatoryContextValue(STORE_NAME);
        String storeOwner = getMandatoryContextValue(STORE_OWNER);
        String storeFqdn = getMandatoryContextValue(STORE_FQDN);
        boolean storeTopologyEnabled = getContextValue(STORE_TOPOLOGY_ENABLED, Boolean.class, true);
        String dbContainerName = getMandatoryContextValue(STORE_DB_CONTAINER_NAME);
        String dbName = getMandatoryContextValue(STORE_DB_NAME);
        String dbUser = getMandatoryContextValue(STORE_DB_USER);
        String dbPass = getMandatoryContextValue(STORE_DB_PASSWORD);

        Optional<Container> container = client.listContainersCmd().withShowAll(true).withNameFilter(List.of(containerName)).exec().stream()
                .filter(c -> Arrays.asList(c.getNames()).contains("/" + containerName)).findFirst();
        if (container.isEmpty()) {
            CreateContainerResponse response = client.createContainerCmd(imageName)
                    .withName(containerName)
                    .withHostName(containerName)
                    .withEnv(
                            "QUARKUS_HTTP_PORT=8080",
                            "STORE.ROOT=/home/jboss",
                            "STORE.AUTH.OWNER=" + storeOwner,
                            "STORE.TOPOLOGY.ENABLED=" + storeTopologyEnabled,
                            "STORE.TOPOLOGY.HOST=consul",
                            "STORE.TOPOLOGY.PORT=8500",
                            "STORE.TOPOLOGY.SERVICE.HOST=" + storeFqdn,
                            "QUARKUS.DATASOURCE.USERNAME=" + dbUser,
                            "QUARKUS.DATASOURCE.PASSWORD=" + dbPass,
                            "QUARKUS.DATASOURCE.JDBC.URL=jdbc:postgresql://" + dbContainerName + ":5432/" + dbName
                    )
                    .withLabels(Map.of(
                            "traefik.enable", "true",
                            "traefik.docker.network", networkName,
                            "traefik.http.routers." + storeName + ".rule", "Host(`" + storeFqdn + "`)",
                            "traefik.http.routers." + storeName + ".entrypoints", "websecure",
                            "traefik.http.routers." + storeName + ".tls", "true",
                            "traefik.http.routers." + storeName + "-http.rule", "Host(`" + storeFqdn + "`)",
                            "traefik.http.routers." + storeName + "-http.entrypoints", "web",
                            "traefik.http.routers." + storeName + "-http.middlewares", "redirect-to-https",
                            "traefik.http.middlewares.redirect-to-https.redirectscheme.scheme", "https",
                            "traefik.http.services." + storeName + ".loadbalancer.server.port", "8080"
                    ))
                    .withHostConfig(HostConfig.newHostConfig()
                            .withNetworkMode(networkName)
                            .withExtraHosts("auth.mbyte.fr:172.25.0.2")
                            .withBinds(new Bind(volumeName, new Volume("/home/jboss"))))
                    .exec();
            if (response.getId() == null) {
                this.fail(String.format("Failed to create store container with name: '%s'", containerName));
                throw new TaskException("Store container creation failed for name: " + containerName + ", warnings: " + String.join(", ",
                        response.getWarnings()));
            }
            this.complete(String.format("Store container created with id: '%s'", response.getId()));
        } else {
            this.log(String.format("Found existing store container with name: '%s', id: '%s'", container.get().getNames()[0], container.get().getId()));
            InspectContainerResponse inspect = client.inspectContainerCmd(container.get().getId()).exec();
            if (!imageName.equals(inspect.getConfig().getImage())) {
                this.fail(String.format("Existing container image '%s' does not match expected '%s'", inspect.getConfig().getImage(), imageName));
                throw new TaskException("Existing container image " + inspect.getConfig().getImage() + " does not match expected " + imageName);
            }
            if (!containerName.equals(inspect.getConfig().getHostName())) {
                this.fail(String.format("Existing container hostname '%s' does not match expected '%s'", inspect.getConfig().getHostName(), containerName));
                throw new TaskException("Existing container hostname " + inspect.getConfig().getHostName() + " does not match expected " + containerName);
            }
            List<String> envVars = inspect.getConfig().getEnv() != null ? Arrays.asList(inspect.getConfig().getEnv()) : List.of();
            List<String> expectedEnv = Arrays.asList(
                    "QUARKUS_HTTP_PORT=8080",
                    "STORE.ROOT=/home/jboss",
                    "STORE.AUTH.OWNER=" + storeOwner,
                    "STORE.TOPOLOGY.ENABLED=" + storeTopologyEnabled,
                    "STORE.TOPOLOGY.HOST=consul",
                    "STORE.TOPOLOGY.PORT=8500",
                    "STORE.TOPOLOGY.SERVICE.HOST=" + storeFqdn,
                    "QUARKUS.DATASOURCE.USERNAME=" + dbUser,
                    "QUARKUS.DATASOURCE.PASSWORD=" + dbPass,
                    "QUARKUS.DATASOURCE.JDBC.URL=jdbc:postgresql://" + dbContainerName + ":5432/" + dbName
            );
            if (!envVars.containsAll(expectedEnv)) {
                this.fail("Existing container environment variables do not match expected");
                throw new TaskException("Existing container environment variables do not match expected");
            }
            Map<String, String> labels = inspect.getConfig().getLabels();
            Map<String, String> expectedLabels = Map.of(
                    "traefik.enable", "true",
                    "traefik.docker.network", "mbyte",
                    "traefik.http.routers." + storeOwner + ".rule", "Host(`" + storeFqdn + "`)",
                    "traefik.http.routers." + storeOwner + ".entrypoints", "http",
                    "traefik.http.routers." + storeOwner + ".service", storeOwner + "-http",
                    "traefik.http.services." + storeOwner + "-http.loadbalancer.server.port", "8080"
            );
            if (labels == null || !labels.entrySet().containsAll(expectedLabels.entrySet())) {
                this.fail("Existing container labels do not match expected");
                throw new TaskException("Existing container labels do not match expected");
            }
            if (!networkName.equals(inspect.getHostConfig().getNetworkMode())) {
                this.fail(String.format("Existing container network mode '%s' does not match expected '%s'", inspect.getHostConfig().getNetworkMode(), networkName));
                throw new TaskException("Existing container network mode " + inspect.getHostConfig().getNetworkMode() + " does not match expected " + networkName);
            }
            List<Bind> binds = inspect.getHostConfig().getBinds() != null ? Arrays.asList(inspect.getHostConfig().getBinds()) : null;
            boolean volumeMatch = binds != null && binds.stream().anyMatch(bind -> volumeName.equals(bind.getPath()) && "/home/jboss".equals(bind.getVolume().getPath()));
            if (!volumeMatch) {
                this.fail(String.format("Existing container volume bind does not match expected: volume='%s' to '/home/jboss'", volumeName));
                throw new TaskException("Existing container volume bind does not match expected: volume='" + volumeName + "' to '/home/jboss'");
            }
            this.complete(String.format("Store container already exists for name: '%s' with id: '%s'", containerName, container.get().getId()));
        }

    }

}
