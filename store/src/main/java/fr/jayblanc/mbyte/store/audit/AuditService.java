package fr.jayblanc.mbyte.store.audit;

import fr.jayblanc.mbyte.store.audit.entity.AuditEvent;
import java.util.List;

public interface AuditService {
    void save(AuditEvent auditEvent);

    List<AuditEvent> list(int limit);
}
