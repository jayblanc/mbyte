package fr.jayblanc.mbyte.store.audit;

import fr.jayblanc.mbyte.store.audit.entity.AuditAction;

public class AuditActionMapper {
    public static AuditAction from(String method, String path, boolean download) {
        if (path == null) return AuditAction.VIEW_NODE;
        String p = path.toLowerCase();
        if (p.startsWith("api/nodes")) {
            if ("get".equalsIgnoreCase(method) && p.endsWith("/content")) {
                return download ? AuditAction.DOWNLOAD : AuditAction.VIEW_NODE;
            }
            if ("get".equalsIgnoreCase(method) && p.contains("/children")) return AuditAction.LIST_CHILDREN;
            if ("get".equalsIgnoreCase(method)) return AuditAction.VIEW_NODE;
            if ("post".equalsIgnoreCase(method)) return AuditAction.CREATE_FOLDER;
            if ("put".equalsIgnoreCase(method)) return AuditAction.UPDATE_FILE;
            if ("delete".equalsIgnoreCase(method)) return AuditAction.DELETE;
        }
        if (p.startsWith("api/search")) return AuditAction.SEARCH;
        if (p.startsWith("api/network")) return AuditAction.NETWORK_INFO;

        return AuditAction.VIEW_NODE;
    }
}
