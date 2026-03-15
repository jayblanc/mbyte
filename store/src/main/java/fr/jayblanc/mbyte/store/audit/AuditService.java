package fr.jayblanc.mbyte.store.audit;

import fr.jayblanc.mbyte.store.audit.entity.AuditEvent;

public interface AuditService {
    void save(AuditEvent auditEvent);
}
