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
