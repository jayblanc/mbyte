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

import fr.jayblanc.mbyte.manager.api.dto.CommandDescriptor;
import fr.jayblanc.mbyte.manager.core.*;
import fr.jayblanc.mbyte.manager.core.entity.Application;
import fr.jayblanc.mbyte.manager.notification.NotificationServiceException;
import fr.jayblanc.mbyte.manager.process.ProcessAlreadyRunningException;
import fr.jayblanc.mbyte.manager.process.ProcessEngine;
import fr.jayblanc.mbyte.manager.process.ProcessNotFoundException;
import fr.jayblanc.mbyte.manager.process.entity.Process;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("apps")
public class AppsResource {

    private static final Logger LOGGER = Logger.getLogger(AppsResource.class.getName());

    @Inject CoreService core;
    @Inject ProcessEngine engine;
    @Inject ApplicationCommandProvider commandProvider;
    @Inject ApplicationDescriptorRegistry descriptorRegistry;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Application> listApps(@QueryParam("owner") String owner) throws AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /api/apps?owner={0}", owner);
        return core.listApps(owner);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApp(@FormParam("type") String type, @FormParam("name") String name, @Context UriInfo uriInfo)
            throws ApplicationDescriptorNotFoundException, NotificationServiceException {
        LOGGER.log(Level.INFO, "POST /api/apps");
        String id = core.createApp(type, name);
        LOGGER.log(Level.INFO, "Application created with id: {0}", id);
        URI location = uriInfo.getBaseUriBuilder().path(AppsResource.class).path(id).build();
        return Response.created(location).entity(java.util.Map.of("id", id)).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Application getApp(@PathParam("id") String id) throws ApplicationNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /api/apps/{0}", id);
        return core.getApp(id);
    }

    @GET
    @Path("{id}/commands")
    @Produces(MediaType.APPLICATION_JSON)
    public List<CommandDescriptor> listCommands(@PathParam("id") String id) throws ApplicationNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /api/apps/{0}/commands", id);
        Application app = core.getApp(id);
        return commandProvider.listCommandsForAppType(app.getType()).stream().map(CommandDescriptor::fromApplicationCommand).toList();
    }

    @POST
    @Path("{id}/procs")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String createProc(@PathParam("id") String id, @FormParam("name") String name)
            throws ApplicationNotFoundException, AccessDeniedException, EnvironmentNotFoundException, ApplicationCommandNotFoundException,
            ProcessAlreadyRunningException, NotificationServiceException {
        LOGGER.log(Level.INFO, "POST /api/apps/{0}/procs", id);
        return core.runAppCommand(id, name, null);
    }

    @GET
    @Path("{id}/procs")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Process> listProcsForApp(@PathParam("id") String id, @QueryParam("active") @DefaultValue("true") boolean active)
            throws ApplicationNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /api/apps/{0}/procs?active={1}", new Object[] { id, active });
        Application app = core.getApp(id);
        if (active) {
            return engine.findRunningProcessesForApp(app.getId());
        }
        return engine.findAllProcessesForApp(app.getId());
    }

    @GET
    @Path("{id}/procs/{pid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Process getProcsForApp(@PathParam("id") String id, @PathParam("pid") String pid)
            throws ApplicationNotFoundException, AccessDeniedException, ProcessNotFoundException {
        LOGGER.log(Level.INFO, "GET /api/apps/{0}/procs/{1}", new Object[] { id, pid });
        Application app = core.getApp(id);
        Process proc = engine.getProcess(pid);
        if ( !proc.getAppId().equals(app.getId()) ) {
            throw new ProcessNotFoundException("Process with id: " + pid + " does not exists for application with id: " + id);
        }
        return proc;
    }
}
