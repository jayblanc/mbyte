package fr.jayblanc.mbyte.manager.api.resources;

import fr.jayblanc.mbyte.manager.server.ServerRegistry;
import fr.jayblanc.mbyte.manager.server.entity.Server;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API for managing and monitoring Docker servers.
 */
@Path("servers")
public class ServersResource {

    private static final Logger LOGGER = Logger.getLogger(ServersResource.class.getName());

    @Inject
    ServerRegistry serverRegistry;

    /**
     * Get all configured servers.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllServers() {
        LOGGER.log(Level.INFO, "GET /api/servers");
        List<Server> servers = serverRegistry.getAllServers();
        return Response.ok(servers).build();
    }

    /**
     * Get all available (enabled and online) servers.
     */
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvailableServers() {
        LOGGER.log(Level.INFO, "GET /api/servers/available");
        List<Server> servers = serverRegistry.getEnabledServers();
        return Response.ok(servers).build();
    }

    /**
     * Get a specific server by ID.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServer(@PathParam("id") String serverId) {
        LOGGER.log(Level.INFO, "GET /api/servers/{0}", serverId);
        return serverRegistry.getServerById(serverId)
                .map(server -> Response.ok(server).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Server not found: " + serverId))
                        .build());
    }

    /**
     * Get health status of a specific server.
     */
    @GET
    @Path("{id}/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServerHealth(@PathParam("id") String serverId) {
        LOGGER.log(Level.INFO, "GET /api/servers/{0}/health", serverId);
        return serverRegistry.getServerById(serverId)
                .map(server -> {
                    Server.ServerStatus status = serverRegistry.checkServerHealth(serverId);
                    Map<String, Object> health = new HashMap<>();
                    health.put("serverId", serverId);
                    health.put("status", status);
                    health.put("enabled", server.isEnabled());
                    return Response.ok(health).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Server not found: " + serverId))
                        .build());
    }

    /**
     * Get the number of stores on a specific server.
     */
    @GET
    @Path("{id}/stores/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServerStoreCount(@PathParam("id") String serverId) {
        LOGGER.log(Level.INFO, "GET /api/servers/{0}/stores/count", serverId);
        return serverRegistry.getServerById(serverId)
                .map(server -> {
                    int count = serverRegistry.getStoreCount(serverId);
                    Map<String, Object> result = new HashMap<>();
                    result.put("serverId", serverId);
                    result.put("storeCount", count);
                    result.put("capacity", server.getCapacity());
                    result.put("available", server.getCapacity() == 0 || count < server.getCapacity());
                    return Response.ok(result).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Server not found: " + serverId))
                        .build());
    }
}
