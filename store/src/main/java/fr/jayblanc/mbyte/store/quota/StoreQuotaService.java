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
import jakarta.inject.Inject;

@ApplicationScoped
public class StoreQuotaService {

    @Inject
    StoreQuotaRepository repository;

    public long getMaxStorage() {
        StoreQuota q = repository.getQuota();
        return q != null ? q.maxStorageBytes : 524288000L; // fallback 500MB
    }

    public long getMaxMemory() {
        StoreQuota q = repository.getQuota();
        return q != null ? q.maxMemoryBytes : 268435456L; // fallback 256MB
    }

    public void addMaxMemory(Long maxMemory) {
        repository.addMemoryQuota(maxMemory);
    }

    public void addMaxStorage(Long maxStorage) {
        repository.addStorageQuota(maxStorage);
    }
}