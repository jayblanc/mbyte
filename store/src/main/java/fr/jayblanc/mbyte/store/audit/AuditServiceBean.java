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
package fr.jayblanc.mbyte.store.audit;

import fr.jayblanc.mbyte.store.audit.entity.AuditEvent;
import fr.jayblanc.mbyte.store.auth.AuthenticationConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class AuditServiceBean implements AuditService {
    @Inject EntityManager em;
    @Inject AuthenticationConfig authConfig;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void save(AuditEvent auditEvent) {
        em.persist(auditEvent);
    }

    @Override
    public List<AuditEvent> list(int limit) {
        return em.createQuery(
                        "SELECT a FROM AuditEvent a WHERE a.storeId = :storeId ORDER BY a.timeStamp DESC",
                        AuditEvent.class)
                .setParameter("storeId", authConfig.owner())
                .setMaxResults(limit)
                .getResultList();
    }
}
