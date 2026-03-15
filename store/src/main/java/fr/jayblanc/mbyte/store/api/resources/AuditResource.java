package fr.jayblanc.mbyte.store.api.resources;

import fr.jayblanc.mbyte.store.audit.AuditService;
import fr.jayblanc.mbyte.store.audit.entity.AuditEvent;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/audits")
@Produces(MediaType.APPLICATION_JSON)
public class AuditResource {
    @Inject
    AuditService auditService;

    @GET
    // @RolesAllowed("admin")
    public List<AuditEvent> list(@QueryParam("limit") @DefaultValue("100") int limit) {
        if (limit > 500) limit = 500;
        return auditService.list(limit);
    }
}
