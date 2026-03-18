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
