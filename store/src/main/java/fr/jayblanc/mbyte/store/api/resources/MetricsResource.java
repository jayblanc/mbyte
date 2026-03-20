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

package fr.jayblanc.mbyte.store.api.resources;

import fr.jayblanc.mbyte.store.metrics.MetricsService;
import fr.jayblanc.mbyte.store.quota.StoreQuotaService;
import io.vertx.core.spi.metrics.Metrics;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("metrics")
public class MetricsResource {

    private static final Logger LOGGER = Logger.getLogger(MetricsResource.class.getName());

    @Inject
    StoreQuotaService quotaService;

    @Inject
    fr.jayblanc.mbyte.store.data.DataStoreConfig dataStoreConfig;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getMetrics() {
        LOGGER.log(Level.INFO, "GET /api/metrics");
        Map<String, Object> response = new HashMap<>();
        long mem = quotaService.getMaxMemory();
        long rom = quotaService.getMaxStorage();
        response.put("totalMemory", mem);
        response.put("storeTotalBytes", rom);
        return response;
    }

    // DTO pour recevoir les nouvelles valeurs
    public static class QuotaUpdateRequest {
        public Long maxMemory;   // en octets
        public Long maxStorage;  // en octets
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateQuotas(QuotaUpdateRequest request) {
        LOGGER.info("Updating quotas: memory=" + request.maxMemory + ", storage=" + request.maxStorage);

        try {
            if (request.maxMemory != null) {
                quotaService.addMaxMemory(request.maxMemory);
            }
            if (request.maxStorage != null) {
                quotaService.addMaxStorage(request.maxStorage);
            }
            return Response.ok()
                    .entity(getMetrics())
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Failed to update quotas: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to update quotas")
                    .build();
        }
    }
}
