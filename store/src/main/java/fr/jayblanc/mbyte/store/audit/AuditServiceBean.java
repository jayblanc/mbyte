package fr.jayblanc.mbyte.store.audit;

import fr.jayblanc.mbyte.store.audit.entity.AuditEvent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

public class AuditServiceBean implements AuditService {
    @Inject
    EntityManager em;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void save(AuditEvent auditEvent) {
        em.persist(auditEvent);
    }
}
