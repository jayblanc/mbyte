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
package fr.jayblanc.mbyte.manager.api.resources;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.management.OperatingSystemMXBean;

@Path("metrics")
public class MetricsResource {

    private static final Logger LOGGER = Logger.getLogger(MetricsResource.class.getName());

    @Inject
    DockerClient dockerClient;

    @GET
    @Path("{containerName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getMetrics(@PathParam("containerName") String containerName) {
        LOGGER.log(Level.INFO, "GET /api/metrics/" + containerName);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> systemMetrics = new HashMap<>();

        // 🔹 CPU + mémoire du host
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemCpuLoad();
            long totalMem = osBean.getTotalPhysicalMemorySize();
            long freeMem = osBean.getFreePhysicalMemorySize();

            systemMetrics.put("cpuLoad", cpuLoad >= 0 ? cpuLoad : 0);
            systemMetrics.put("totalMemory", totalMem);
            systemMetrics.put("freeMemory", freeMem);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to calculate system CPU/memory", e);
            systemMetrics.put("cpuLoad", 0);
            systemMetrics.put("totalMemory", 0);
            systemMetrics.put("freeMemory", 0);
        }

        // 🔹 Stockage du store
        try {
            // récupère le nom du volume depuis le container
            String volumeName = dockerClient.inspectContainerCmd(containerName)
                    .exec()
                    .getMounts().get(0)
                    .getName();

            // crée un container temporaire alpine pour mesurer le volume
            ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerName)
                    .withCmd("du", "-sb", "/home/jboss")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec();

            ByteArrayOutputStream output = new ByteArrayOutputStream();

            dockerClient.execStartCmd(exec.getId())
                    .exec(new ExecStartResultCallback(output, System.err))
                    .awaitCompletion();

            String resultStr = output.toString().trim();
            long storeUsedBytes = Long.parseLong(resultStr.split("\\s+")[0]);
            systemMetrics.put("storeUsedBytes", storeUsedBytes);

            // Pour Docker volumes locaux, on peut estimer total à 1To ou 0 si host info non dispo
            systemMetrics.put("storeTotalBytes", 1_000_000_000_000L);
            systemMetrics.put("storeFreeBytes", 1_000_000_000_000L - storeUsedBytes);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to calculate store size for container " + containerName, e);
            systemMetrics.put("storeUsedBytes", 0L);
            systemMetrics.put("storeTotalBytes", 0L);
            systemMetrics.put("storeFreeBytes", 0L);
        }

        // 🔹 metrics applicatives (vide par défaut)
        result.put("metrics", new HashMap<>());
        result.put("latestMetrics", new HashMap<>());
        result.put("system", systemMetrics);

        LOGGER.log(Level.INFO, "Metrics returned for container " + containerName);

        return result;
    }
}