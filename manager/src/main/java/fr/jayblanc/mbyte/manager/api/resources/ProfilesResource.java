package fr.jayblanc.mbyte.manager.api.resources;

import fr.jayblanc.mbyte.manager.auth.AuthenticationService;
import fr.jayblanc.mbyte.manager.auth.entity.Profile;
import fr.jayblanc.mbyte.manager.core.CoreService;
import fr.jayblanc.mbyte.manager.core.CoreServiceException;
import fr.jayblanc.mbyte.manager.core.StoreNotFoundException;
import fr.jayblanc.mbyte.manager.core.entity.Store;
import fr.jayblanc.mbyte.manager.server.ServerRegistry;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("profiles")
public class ProfilesResource {

    private static final Logger LOGGER = Logger.getLogger(ProfilesResource.class.getName());

    @Inject AuthenticationService auth;
    @Inject CoreService core;
    @Inject ServerRegistry serverRegistry;
    @Inject Template profile;

    @GET
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
    public Response profiles(@Context UriInfo uriInfo) {
        LOGGER.log(Level.INFO, "GET /api/profiles");
        String connectedId = auth.getConnectedIdentifier();
        URI root = uriInfo.getRequestUriBuilder().path(connectedId).build();
        return Response.seeOther(root).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response profile(@PathParam("id") String id, @Context UriInfo uriInfo) {
        LOGGER.log(Level.INFO, "GET /api/profiles/{0}", id);
        Profile profile = auth.getConnectedProfile();
        return Response.ok(profile).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance profileView(@PathParam("id") String id, @Context UriInfo uriInfo) {
        LOGGER.log(Level.INFO, "GET /api/profiles/{0} (html)", id);
        TemplateInstance view = profile.data("profile", auth.getConnectedProfile());
        // Multi-store: passer la liste des stores au template
        List<Store> stores = core.getUserStores();
        view = view.data("stores", stores);
        // Multi-server: passer la liste des serveurs disponibles au template
        view = view.data("servers", serverRegistry.getAllServers().stream()
                .filter(fr.jayblanc.mbyte.manager.server.entity.Server::isEnabled)
                .collect(java.util.stream.Collectors.toList()));
        // Rétrocompatibilité: passer aussi le premier store comme "store"
        if (!stores.isEmpty()) {
            view = view.data("store", stores.get(0));
        }
        return view;
    }

    @GET
    @Path("{id}/store")
    @Produces(MediaType.APPLICATION_JSON)
    public Store getProfileStore(@PathParam("id") String id) throws StoreNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "GET /api/profiles/{0}/store", id);
        return core.getConnectedUserStore();
    }

    @GET
    @Path("{id}/store/log")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProfileStoreLog(@PathParam("id") String id) throws StoreNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "GET /api/profiles/{0}/store/log", id);
        return core.getConnectedUserStore().getLog();
    }

    @POST
    @Path("{id}/store")
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createProfileStore(@PathParam("id") String id, MultivaluedMap<String, String> form, @Context UriInfo uriInfo) {
        LOGGER.log(Level.INFO, "POST /api/profiles/{0}/store", id);
        String name = form.getFirst("name");
        String serverId = form.getFirst("serverId");
        Store store = core.createStore(name, serverId);
        LOGGER.log(Level.INFO, "Store created with id: {0} on server: {1}",
                new Object[]{store.getId(), store.getServerId()});
        String connectedId = auth.getConnectedIdentifier();
        URI root = uriInfo.getBaseUriBuilder().path(ProfilesResource.class).path(connectedId).build();
        return Response.seeOther(root).build();
    }

    // ========== Multi-Store Support ==========

    @GET
    @Path("{id}/stores")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listStores(@PathParam("id") String id) {
        LOGGER.log(Level.INFO, "GET /api/profiles/{0}/stores", id);
        List<Store> stores = core.getUserStores();
        return Response.ok(stores).build();
    }

    @GET
    @Path("{id}/stores/{storeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStoreById(@PathParam("id") String id, @PathParam("storeId") String storeId) {
        LOGGER.log(Level.INFO, "GET /api/profiles/{0}/stores/{1}", new Object[]{id, storeId});
        try {
            Store store = core.getStoreById(storeId);
            return Response.ok(store).build();
        } catch (StoreNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Store not found: " + storeId).build();
        }
    }

    @POST
    @Path("{id}/stores")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createStore(@PathParam("id") String id, MultivaluedMap<String, String> form, @Context UriInfo uriInfo) {
        LOGGER.log(Level.INFO, "POST /api/profiles/{0}/stores", id);
        String name = form.getFirst("name");
        String serverId = form.getFirst("serverId");
        Store store = core.createStore(name, serverId);
        LOGGER.log(Level.INFO, "Store created with id: {0} on server: {1}",
                new Object[]{store.getId(), store.getServerId()});
        String connectedId = auth.getConnectedIdentifier();
        URI root = uriInfo.getBaseUriBuilder().path(ProfilesResource.class).path(connectedId).build();
        return Response.seeOther(root).build();
    }

    @PUT
    @Path("{id}/stores/{storeId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response renameStore(@PathParam("id") String id, @PathParam("storeId") String storeId, MultivaluedMap<String, String> form) {
        LOGGER.log(Level.INFO, "PUT /api/profiles/{0}/stores/{1}", new Object[]{id, storeId});
        try {
            Store store = core.renameStore(storeId, form.getFirst("name"));
            return Response.ok(store).build();
        } catch (StoreNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Store not found: " + storeId).build();
        }
    }

    @DELETE
    @Path("{id}/stores/{storeId}")
    public Response deleteStore(@PathParam("id") String id, @PathParam("storeId") String storeId) {
        LOGGER.log(Level.INFO, "DELETE /api/profiles/{0}/stores/{1}", new Object[]{id, storeId});
        try {
            core.deleteStore(storeId);
            return Response.noContent().build();
        } catch (StoreNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Store not found: " + storeId).build();
        } catch (CoreServiceException e) {
            LOGGER.log(Level.WARNING, "Error deleting store: {0}", new Object[]{storeId, e});
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error deleting store: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("{id}/stores/count")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStoreCount(@PathParam("id") String id) {
        LOGGER.log(Level.INFO, "GET /api/profiles/{0}/stores/count", id);
        long count = core.getStoreCount();
        return Response.ok("{\"count\":" + count + "}").build();
    }

    // ========== Multi-Server Replication ==========

    @POST
    @Path("{id}/stores/{storeId}/replicate")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response replicateStore(@PathParam("id") String id, @PathParam("storeId") String storeId,
                                   MultivaluedMap<String, String> form, @Context UriInfo uriInfo) {
        LOGGER.log(Level.INFO, "POST /api/profiles/{0}/stores/{1}/replicate", new Object[]{id, storeId});
        String targetServerId = form.getFirst("serverId");
        try {
            Store replica = core.replicateStore(storeId, targetServerId);
            LOGGER.log(Level.INFO, "Store replicated: {0} -> server {1}",
                    new Object[]{replica.getId(), replica.getServerId()});
            String connectedId = auth.getConnectedIdentifier();
            URI root = uriInfo.getBaseUriBuilder().path(ProfilesResource.class).path(connectedId).build();
            return Response.seeOther(root).build();
        } catch (StoreNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Store not found: " + storeId).build();
        } catch (CoreServiceException e) {
            LOGGER.log(Level.WARNING, "Error replicating store: {0}", new Object[]{storeId, e});
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

}
