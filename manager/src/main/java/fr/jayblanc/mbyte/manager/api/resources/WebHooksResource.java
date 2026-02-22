package fr.jayblanc.mbyte.manager.api.resources;

import fr.jayblanc.mbyte.manager.core.AccessDeniedException;
import fr.jayblanc.mbyte.manager.core.services.WebHookService;
import fr.jayblanc.mbyte.manager.core.entity.WebHook;
import fr.jayblanc.mbyte.manager.core.exceptions.WebHookNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("webhooks")
public class WebHooksResource {

    private static final Logger LOGGER = Logger.getLogger(WebHooksResource.class.getName());

    @Inject WebHookService webhookService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<WebHook> listWebHooks(@QueryParam("owner") String owner) throws AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /api/webhooks?owner={0}", owner);
        return webhookService.listWebHooks(owner);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createWebHook(
            @FormParam("owner") String owner,
            @FormParam("url") String url,
            @FormParam("events") List<String> events,
            @Context UriInfo uriInfo) throws AccessDeniedException {

        LOGGER.log(Level.INFO, "POST /api/webhooks for owner: {0}", owner);

        // The service generates the ID and the Secret internally
        String id = webhookService.createWebHook(owner, url, events);

        LOGGER.log(Level.INFO, "WebHook created with id: {0}", id);
        URI location = uriInfo.getBaseUriBuilder().path(WebHooksResource.class).path(id).build();
        return Response.created(location).entity(java.util.Map.of("id", id)).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public WebHook getWebHook(@PathParam("id") String id)
            throws WebHookNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /api/webhooks/{0}", id);
        return webhookService.getWebHook(id);
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public WebHook updateWebHook(
            @PathParam("id") String id,
            @FormParam("url") String url,
            @FormParam("active") boolean active,
            @FormParam("events") List<String> events)
            throws WebHookNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "PUT /api/webhooks/{0}", id);
        return webhookService.updateWebHook(id, url, active, events);
    }

    @DELETE
    @Path("{id}")
    public Response deleteWebHook(@PathParam("id") String id)
            throws WebHookNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "DELETE /api/webhooks/{0}", id);
        webhookService.deleteWebHook(id);
        return Response.noContent().build();
    }
}