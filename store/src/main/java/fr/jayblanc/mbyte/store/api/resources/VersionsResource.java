package fr.jayblanc.mbyte.store.api.resources;

import fr.jayblanc.mbyte.store.api.dto.FileVersionDto;
import fr.jayblanc.mbyte.store.api.dto.VersionCreateDto;
import fr.jayblanc.mbyte.store.api.filter.OnlyOwner;
import fr.jayblanc.mbyte.store.auth.AuthenticationService;
import fr.jayblanc.mbyte.store.versioning.VersioningService;
import fr.jayblanc.mbyte.store.versioning.entity.FileVersion;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("versions")
@OnlyOwner
public class VersionsResource {

    private static final Logger LOGGER = Logger.getLogger(VersionsResource.class.getName());

    @Inject VersioningService versioningService;
    @Inject AuthenticationService auth;

    @GET
    @Path("node/{nodeId}")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHistory(@PathParam("nodeId") String nodeId) {
        LOGGER.log(Level.INFO, "GET /api/versions/node/{0}", nodeId);
        List<FileVersion> versions = versioningService.getHistory(nodeId);
        List<FileVersionDto> dtos = versions.stream().map(FileVersionDto::fromEntity).toList();
        return Response.ok(dtos).build();
    }

    @POST
    @Path("node/{nodeId}")
    @Transactional(Transactional.TxType.REQUIRED)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVersion(
            @PathParam("nodeId") String nodeId,
            @MultipartForm VersionCreateDto dto) {
        LOGGER.log(Level.INFO, "POST /api/versions/node/{0}", nodeId);
        String author = auth.getConnectedProfile().getUsername();
        FileVersion version = versioningService.createVersion(
                nodeId, dto.getName(), dto.getFile(), dto.getMimetype(), dto.getSize(), author, dto.getComment());
        return Response.ok(FileVersionDto.fromEntity(version)).build();
    }

    @GET
    @Path("{versionId}")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersion(@PathParam("versionId") String versionId) {
        LOGGER.log(Level.INFO, "GET /api/versions/{0}", versionId);
        FileVersion version = versioningService.getVersion(versionId);
        if (version == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(FileVersionDto.fromEntity(version)).build();
    }

    @GET
    @Path("{versionId}/content")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces(MediaType.WILDCARD)
    public Response getVersionContent(@PathParam("versionId") String versionId) {
        LOGGER.log(Level.INFO, "GET /api/versions/{0}/content", versionId);
        FileVersion version = versioningService.getVersion(versionId);
        if (version == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        InputStream content = versioningService.getVersionContent(versionId);
        if (content == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(content)
                .header("Content-Type", version.getMimetype())
                .header("Content-Length", version.getSize())
                .header("Content-Disposition", "filename=" + version.getNodeName())
                .build();
    }

    @DELETE
    @Path("{versionId}")
    @Transactional(Transactional.TxType.REQUIRED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVersion(@PathParam("versionId") String versionId) {
        LOGGER.log(Level.INFO, "DELETE /api/versions/{0}", versionId);
        versioningService.deleteVersion(versionId);
        return Response.noContent().build();
    }
}
