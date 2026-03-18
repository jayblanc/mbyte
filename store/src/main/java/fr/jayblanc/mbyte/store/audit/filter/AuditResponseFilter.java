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

import fr.jayblanc.mbyte.store.audit.AuditService;
import fr.jayblanc.mbyte.store.audit.entity.AuditEvent;
import fr.jayblanc.mbyte.store.audit.entity.AuditStatus;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Priority(Priorities.USER + 1)
public class AuditResponseFilter implements ContainerResponseFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditResponseFilter.class);
    @Inject AuditService auditService;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object payload = requestContext.getProperty(AuditRequestFilter.AUDIT_CONTEXT_KEY);
        if (!(payload instanceof AuditEvent event)) {
            log.warn("Nous n'avons pas d'évent");
            return;
        }

        event.setStatus(responseContext.getStatus() >= 400 ? AuditStatus.FAILURE : AuditStatus.SUCCESS);

        auditService.save(event);
    }
}
