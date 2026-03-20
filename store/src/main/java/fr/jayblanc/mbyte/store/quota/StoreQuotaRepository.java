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
package fr.jayblanc.mbyte.store.quota;

import fr.jayblanc.mbyte.store.quota.entity.StoreQuota;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class StoreQuotaRepository {

    @PersistenceContext
    EntityManager em;

    public StoreQuota getQuota() {
        return em.createQuery("SELECT q FROM StoreQuota q ORDER BY q.id ASC", StoreQuota.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void save(StoreQuota quota) {
        em.persist(quota);
    }

    @Transactional
    public void addStorageQuota(Long toAdd) {
        em.createQuery("UPDATE StoreQuota q SET q.maxStorageBytes = q.maxStorageBytes + :toAdd")
                .setParameter("toAdd", toAdd)
                .executeUpdate();
    }

    @Transactional
    public void addMemoryQuota(Long toAdd) {
        em.createQuery("UPDATE StoreQuota q SET q.maxMemoryBytes = q.maxMemoryBytes + :toAdd")
                .setParameter("toAdd", toAdd)
                .executeUpdate();
    }
}