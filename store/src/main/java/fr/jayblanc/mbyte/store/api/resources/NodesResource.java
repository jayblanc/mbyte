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

import fr.jayblanc.mbyte.store.api.dto.CollectionDto;
import fr.jayblanc.mbyte.store.api.dto.NodeCreateDto;
import fr.jayblanc.mbyte.store.api.dto.NodeDto;
import fr.jayblanc.mbyte.store.api.filter.OnlyOwner;
import fr.jayblanc.mbyte.store.auth.AuthenticationService;
import fr.jayblanc.mbyte.store.data.exception.DataNotFoundException;
import fr.jayblanc.mbyte.store.data.exception.DataStoreException;
import fr.jayblanc.mbyte.store.files.FileService;
import fr.jayblanc.mbyte.store.files.entity.Node;
import fr.jayblanc.mbyte.store.files.exceptions.*;
import fr.jayblanc.mbyte.store.notification.NotificationServiceException;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("nodes")
@OnlyOwner
public class NodesResource {

    private static final Logger LOGGER = Logger.getLogger(NodesResource.class.getName());

    @Inject FileService service;
    @Inject AuthenticationService auth;

    @GET
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response root(@Context UriInfo uriInfo) throws NodeNotFoundException {
        LOGGER.log(Level.INFO, "GET /api/nodes");
        Node node = service.get("");
        URI root = uriInfo.getRequestUriBuilder().path(node.getId()).build();
        return Response.seeOther(root).build();
    }

    @GET
    @Path("{id}")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces({MediaType.APPLICATION_JSON})
    public Node get(@PathParam("id") final String id) throws NodeNotFoundException {
        LOGGER.log(Level.INFO, "GET /api/nodes/{0}", id);
        return service.get(id);
    }

    @GET
    @Path("{id}/path")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces({MediaType.APPLICATION_JSON})
    public List<Node> path(@PathParam("id") final String id) throws NodeNotFoundException {
        LOGGER.log(Level.INFO, "GET /api/nodes/{0}/path", id);
        return service.path(id);
    }

    @GET
    @Path("{id}/content")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces(MediaType.WILDCARD)
    public Response content(@PathParam("id") final String id, @QueryParam("download") @DefaultValue("false") final boolean download) throws NodeNotFoundException, NodeTypeException,
            DataNotFoundException, DataStoreException {
        LOGGER.log(Level.INFO, "GET /api/nodes/{0}/content", id);
        Node node = service.get(id);
        if (node.getType().equals(Node.Type.BLOB)) {
            return Response.ok(service.getContent(id))
                    .header("Content-Type", node.getMimetype())
                    .header("Content-Length", node.getSize())
                    .header("Content-Disposition", ((download) ? "attachment; " : "") + "filename=" + node.getName()).build();
        } else {
            throw new NodeTypeException("Node is not a file");
        }
    }

    @GET
    @Path("{id}/children")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response children(@PathParam("id") final String id, @QueryParam("limit") @DefaultValue("20") int limit, @QueryParam("offset") @DefaultValue("0") int offset) throws NodeNotFoundException,
            NodeTypeException {
        LOGGER.log(Level.INFO, "GET /api/nodes/{0}/children", id);
        Node node = service.get(id);
        if (node.getType().equals(Node.Type.TREE)) {
            CollectionDto<NodeDto> dto = new CollectionDto<>(limit, offset);
            List<Node> nodes = service.list(node.getId());
            dto.setValues(nodes.stream().skip(offset).limit(limit).map(NodeDto::fromNode).toList());
            dto.setSize(nodes.size());
            dto.setLimit(limit);
            dto.setOffset(offset);
            return Response.ok(dto).build();
        } else {
            throw new NodeTypeException("Node is not a directory");
        }
    }

    @POST
    @Path("{id}")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.MULTIPART_FORM_DATA})
    public Response create(@PathParam("id") final String id, @Valid @MultipartForm NodeCreateDto dto, @Context UriInfo info) throws NodeNotFoundException, NodeTypeException,
            NodeAlreadyExistsException, DataNotFoundException, DataStoreException, NodePersistenceException, NotificationServiceException {
        LOGGER.log(Level.INFO, "POST /api/nodes/{0}", id);
        String nid;
        if (dto.getContent() != null) {
            nid = service.add(id, dto.getName(), new ByteArrayInputStream(dto.getContent()));
        } else if (dto.getData() != null) {
            nid = service.add(id, dto.getName(), dto.getData());
        } else {
            nid = service.add(id, dto.getName());
        }
        URI createdUri = info.getBaseUriBuilder().path(NodesResource.class).path(nid).build();
        return Response.created(createdUri).build();
    }

    @PUT
    @Path("{id}/{name}")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response update(@PathParam("id") final String id, @PathParam("name") String name, @FormParam("data") InputStream data) throws
            NodeNotEmptyException, NodeNotFoundException, NodeTypeException, NodeAlreadyExistsException, DataStoreException, DataNotFoundException, NodePersistenceException, NotificationServiceException {
        LOGGER.log(Level.INFO, "PUT /api/nodes/{0}/{1}", new Object[]{id, name});
        service.remove(id, name);
        service.add(id, name, data);
        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}/{name}")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") final String id, @PathParam("name") final String name) throws NodeNotEmptyException, NodeNotFoundException, NodeTypeException, DataStoreException, NodePersistenceException, NotificationServiceException {
        LOGGER.log(Level.INFO, "DELETE /api/nodes/{0}", name);
        service.remove(id, name);
        return Response.noContent().build();
    }

}
