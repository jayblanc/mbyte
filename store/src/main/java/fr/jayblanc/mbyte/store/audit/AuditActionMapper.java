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

import fr.jayblanc.mbyte.store.audit.entity.AuditAction;

public class AuditActionMapper {
     public static AuditAction from(String method, String path, boolean download, String contentType) {
        if (path == null) return AuditAction.VIEW_NODE;
        String p = path.toLowerCase();
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (p.startsWith("/api/")) {
            p = p.substring(4);
        }

        if (p.startsWith("/nodes")) {
            if ("get".equalsIgnoreCase(method) && p.endsWith("/content")) {
                return download ? AuditAction.DOWNLOAD : AuditAction.VIEW_NODE;
            }
            if ("get".equalsIgnoreCase(method) && p.contains("/children")) return AuditAction.LIST_CHILDREN;
            if ("get".equalsIgnoreCase(method)) return AuditAction.VIEW_NODE;
            if ("post".equalsIgnoreCase(method)) {
                return ct.contains("multipart/form-data") ? AuditAction.UPLOAD : AuditAction.CREATE_FOLDER;
            }
            if ("put".equalsIgnoreCase(method)) return AuditAction.UPDATE_FILE;
            if ("delete".equalsIgnoreCase(method)) return AuditAction.DELETE;
        }
        if (p.startsWith("/search")) return AuditAction.SEARCH;
        if (p.startsWith("/network")) return AuditAction.NETWORK_INFO;

        return AuditAction.VIEW_NODE;
    }
}
