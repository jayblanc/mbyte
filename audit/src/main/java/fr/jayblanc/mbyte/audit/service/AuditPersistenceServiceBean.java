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
package fr.jayblanc.mbyte.audit.service;

import fr.jayblanc.mbyte.audit.model.AuditEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class AuditPersistenceServiceBean implements AuditPersistenceService {

    @Inject EntityManager em;

    @Override
    @Transactional
    public void save(AuditEvent auditEvent) {
        if (auditEvent.getId() == null) {
            auditEvent.setId(java.util.UUID.randomUUID().toString());
        }
        em.persist(auditEvent);
    }

    @Override
    public List<AuditEvent> list(int limit, String storeId) {
        var query = em.createQuery("SELECT a FROM AuditEvent a ORDER BY a.timeStamp DESC", AuditEvent.class);
        return query.setMaxResults(limit).getResultList();
    }
}
