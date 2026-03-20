package fr.jayblanc.mbyte.store.versioning;

import fr.jayblanc.mbyte.store.data.DataStore;
import fr.jayblanc.mbyte.store.data.exception.DataNotFoundException;
import fr.jayblanc.mbyte.store.data.exception.DataStoreException;
import fr.jayblanc.mbyte.store.versioning.entity.FileVersion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class VersioningServiceBean implements VersioningService {

    private static final Logger LOGGER = Logger.getLogger(VersioningServiceBean.class.getName());

    @Inject EntityManager em;
    @Inject DataStore datastore;

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public FileVersion createVersion(String nodeId, String nodeName, InputStream content,
                                     String mimetype, long size, String author, String comment) {
        LOGGER.log(Level.INFO, "Creating version for node: {0} ({1})", new Object[]{nodeId, nodeName});

        long count = getVersionCount(nodeId);

        String contentHash = null;
        long contentSize = size;
        if (content != null) {
            try {
                contentHash = datastore.put(content);
                contentSize = datastore.size(contentHash);
            } catch (DataStoreException | DataNotFoundException e) {
                LOGGER.log(Level.WARNING, "Failed to store version content", e);
                throw new RuntimeException("Failed to store version content", e);
            }
        }

        FileVersion version = new FileVersion();
        version.setId(UUID.randomUUID().toString());
        version.setNodeId(nodeId);
        version.setNodeName(nodeName);
        version.setContent(contentHash);
        version.setMimetype(mimetype);
        version.setSize(contentSize);
        version.setAuthor(author);
        version.setComment(comment);
        version.setCreatedAt(System.currentTimeMillis());
        version.setVersionNumber(count + 1);

        em.persist(version);
        LOGGER.log(Level.INFO, "Version #{0} created for node {1}", new Object[]{version.getVersionNumber(), nodeId});
        return version;
    }

    @Override
    public List<FileVersion> getHistory(String nodeId) {
        return em.createNamedQuery("FileVersion.findByNodeId", FileVersion.class)
                .setParameter("nodeId", nodeId)
                .getResultList();
    }

    @Override
    public FileVersion getVersion(String versionId) {
        return em.find(FileVersion.class, versionId);
    }

    @Override
    public InputStream getVersionContent(String versionId) {
        FileVersion version = getVersion(versionId);
        if (version == null || version.getContent() == null) {
            return null;
        }
        try {
            return datastore.get(version.getContent());
        } catch (DataStoreException | DataNotFoundException e) {
            LOGGER.log(Level.WARNING, "Failed to get version content", e);
            return null;
        }
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void deleteVersion(String versionId) {
        FileVersion version = getVersion(versionId);
        if (version != null) {
            if (version.getContent() != null) {
                try {
                    datastore.delete(version.getContent());
                } catch (DataStoreException e) {
                    LOGGER.log(Level.WARNING, "Failed to delete version content from datastore", e);
                }
            }
            em.remove(version);
        }
    }

    @Override
    public long getVersionCount(String nodeId) {
        return em.createNamedQuery("FileVersion.countByNodeId", Long.class)
                .setParameter("nodeId", nodeId)
                .getSingleResult();
    }
}
