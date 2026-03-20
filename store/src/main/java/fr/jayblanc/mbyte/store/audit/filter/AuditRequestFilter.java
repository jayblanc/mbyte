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
package fr.jayblanc.mbyte.store.audit.filter;

import fr.jayblanc.mbyte.store.audit.AuditActionMapper;
import fr.jayblanc.mbyte.store.audit.entity.AuditEvent;
import fr.jayblanc.mbyte.store.audit.entity.AuditService;
import fr.jayblanc.mbyte.store.audit.entity.AuditStatus;
import fr.jayblanc.mbyte.store.auth.AuthenticationConfig;
import fr.jayblanc.mbyte.store.auth.AuthenticationService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER)
public class AuditRequestFilter implements ContainerRequestFilter {
    public static final String AUDIT_CONTEXT_KEY = "mbyte.audit.event";

    @Inject AuthenticationService authenticationService;
    @Inject AuthenticationConfig authConfig;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        AuditEvent event = new AuditEvent();
        event.setUserId(authenticationService.getConnectedIdentifier());
        event.setStoreId(authConfig.owner());
        event.setMethod(requestContext.getMethod());
        event.setPath(requestContext.getUriInfo().getPath(false));
        event.setService(AuditService.STORE);

        String downloadParam = requestContext.getUriInfo().getQueryParameters().getFirst("download");
        boolean download = Boolean.parseBoolean(downloadParam);
        String contentType = requestContext.getHeaderString("Content-Type");

        event.setAction(AuditActionMapper.from(event.getMethod(), event.getPath(), download, contentType));
        event.setStatus(AuditStatus.SUCCESS);

        requestContext.setProperty(AUDIT_CONTEXT_KEY, event);
    }
}
