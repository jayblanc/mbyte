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
package fr.jayblanc.mbyte.store.quota.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "store_quota")
public class StoreQuota {

    //id car obligatoire mais il n'y aura jamais qu'une ligne dans la table
    @Id
    @Column(name = "max_storage_bytes", nullable = false)
    public Long maxStorageBytes;

    @Column(name = "max_memory_bytes", nullable = false)
    public Long maxMemoryBytes;
}