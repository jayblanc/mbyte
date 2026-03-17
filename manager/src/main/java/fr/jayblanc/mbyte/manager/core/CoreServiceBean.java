package fr.jayblanc.mbyte.manager.core;

import fr.jayblanc.mbyte.manager.auth.AuthenticationService;
import fr.jayblanc.mbyte.manager.core.entity.Store;
import fr.jayblanc.mbyte.manager.server.ServerRegistry;
import fr.jayblanc.mbyte.manager.server.entity.Server;
import fr.jayblanc.mbyte.manager.server.selection.NoServerAvailableException;
import fr.jayblanc.mbyte.manager.store.StoreManager;
import fr.jayblanc.mbyte.manager.store.StoreProviderException;
import fr.jayblanc.mbyte.manager.store.StoreProviderNotFoundException;
import fr.jayblanc.mbyte.manager.topology.TopologyService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CoreServiceBean implements CoreService {

    private static final Logger LOGGER = Logger.getLogger(CoreServiceBean.class.getName());

    @Inject EntityManager em;
    @Inject AuthenticationService authenticationService;
    @Inject StoreManager manager;
    @Inject TopologyService topology;
    @Inject ServerRegistry serverRegistry;

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Store createStore(String name) {
        return createStore(name, null);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Store createStore(String name, String preferredServerId) {
        LOGGER.log(Level.INFO, "Creating new store with name: {0}, preferredServer: {1}",
                new Object[]{name, preferredServerId});
        Store store = new Store();
        store.setId(UUID.randomUUID().toString());
        store.setName(name);
        store.setCreationDate(System.currentTimeMillis());
        store.setOwner(authenticationService.getConnectedProfile().getUsername());
        store.setUsage(0);
        store.setStatus(Store.Status.PENDING);
        try {
            // Select server for deployment
            Server server = manager.selectServerForNewStore(store.getOwner(), store.getName(), preferredServerId);
            store.setServerId(server.getId());
            LOGGER.log(Level.INFO, "Selected server {0} for store {1}", new Object[]{server.getId(), store.getName()});

            // Create store on selected server
            String output = manager.getProvider().createStore(store.getId(), store.getOwner(), store.getName(), server);
            store.setLocation(topology.lookup(store.getName()));
            store.setStatus(Store.Status.AVAILABLE);
            store.setLog(output);
        } catch (NoServerAvailableException e) {
            LOGGER.log(Level.WARNING, "No server available for store creation", e);
            store.setStatus(Store.Status.LOST);
            store.setLog("No server available: " + e.getMessage());
        } catch (StoreProviderException | StoreProviderNotFoundException e) {
            LOGGER.log(Level.WARNING, "Unable to create store, see logs", e);
            store.setStatus(Store.Status.LOST);
            store.setLog("Provider error: " + e.getMessage());
        }
        em.persist(store);
        return store;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Store getConnectedUserStore() throws StoreNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "Getting store for connected user (returning first store)");
        List<Store> stores = getUserStores();
        if (stores.isEmpty()) {
            throw new StoreNotFoundException("No store found for connected user");
        }
        return stores.get(0);  // Retourne le premier store (rétrocompatibilité)
    }

    // ========== Multi-Store Support ==========

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public List<Store> getUserStores() {
        LOGGER.log(Level.INFO, "Getting all stores for connected user");
        String owner = authenticationService.getConnectedProfile().getUsername();
        List<Store> stores = findAllByOwner(owner);
        for (Store store : stores) {
            String location = lookup(store.getName());
            store.setLocation(location != null ? location : "#");
        }
        return stores;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Store getStoreById(String storeId) throws StoreNotFoundException {
        LOGGER.log(Level.INFO, "Getting store by id: {0}", storeId);
        Store store = em.find(Store.class, storeId);
        if (store == null) {
            throw new StoreNotFoundException(storeId);
        }
        // Vérifier que le store appartient à l'utilisateur connecté (sécurité)
        String owner = authenticationService.getConnectedProfile().getUsername();
        if (!store.getOwner().equals(owner)) {
            LOGGER.log(Level.WARNING, "User {0} tried to access store owned by {1}",
                    new Object[]{owner, store.getOwner()});
            throw new StoreNotFoundException(storeId);
        }
        String location = lookup(store.getName());
        store.setLocation(location != null ? location : "#");
        return store;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void deleteStore(String storeId) throws StoreNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "Deleting store: {0}", storeId);
        Store store = getStoreById(storeId);
        try {
            // Use server-aware destroy if serverId is available
            if (store.getServerId() != null) {
                Server server = serverRegistry.getServerById(store.getServerId())
                        .orElseThrow(() -> new CoreServiceException(
                                "Server not found for store: " + store.getServerId()));
                manager.getProvider().destroyStore(store.getId(), server);
            } else {
                // Legacy: store created before multi-server support
                manager.getProvider().destroyStore(store.getId());
            }
        } catch (StoreProviderException | StoreProviderNotFoundException e) {
            LOGGER.log(Level.WARNING, "Error destroying store provider resources", e);
            throw new CoreServiceException("Failed to destroy store: " + e.getMessage());
        }
        em.remove(em.contains(store) ? store : em.merge(store));
        LOGGER.log(Level.INFO, "Store deleted successfully: {0}", storeId);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Store renameStore(String storeId, String newName) throws StoreNotFoundException {
        LOGGER.log(Level.INFO, "Renaming store {0} to: {1}", new Object[]{storeId, newName});
        Store store = getStoreById(storeId);
        store.setName(newName);
        em.merge(store);
        return store;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public long getStoreCount() {
        String owner = authenticationService.getConnectedProfile().getUsername();
        return em.createNamedQuery("Store.countByOwner", Long.class)
            .setParameter("owner", owner)
            .getSingleResult();
    }

    // ========== Multi-Server Replication ==========

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public Store replicateStore(String storeId, String targetServerId) throws StoreNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "Replicating store {0} to server {1}", new Object[]{storeId, targetServerId});
        Store original = getStoreById(storeId);

        // Check if store is already on the target server
        List<Store> existing = em.createNamedQuery("Store.findByOwnerAndNameAndServerId", Store.class)
                .setParameter("owner", original.getOwner())
                .setParameter("name", original.getName())
                .setParameter("serverId", targetServerId)
                .getResultList();
        if (!existing.isEmpty()) {
            throw new CoreServiceException("Store '" + original.getName() + "' already exists on server " + targetServerId);
        }

        // Create a new store with the same name/owner on the target server
        Store replica = new Store();
        replica.setId(UUID.randomUUID().toString());
        replica.setName(original.getName());
        replica.setOwner(original.getOwner());
        replica.setCreationDate(System.currentTimeMillis());
        replica.setUsage(0);
        replica.setStatus(Store.Status.PENDING);
        replica.setServerId(targetServerId);

        try {
            Server server = serverRegistry.getServerById(targetServerId)
                    .orElseThrow(() -> new CoreServiceException("Server not found: " + targetServerId));
            String output = manager.getProvider().createStore(replica.getId(), replica.getOwner(), replica.getName(), server);
            replica.setLocation(topology.lookup(replica.getName()));
            replica.setStatus(Store.Status.AVAILABLE);
            replica.setLog("Replicated from store " + storeId + ". " + output);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to replicate store", e);
            replica.setStatus(Store.Status.LOST);
            replica.setLog("Replication failed: " + e.getMessage());
        }

        em.persist(replica);
        LOGGER.log(Level.INFO, "Store replicated: {0} -> {1} on server {2}",
                new Object[]{storeId, replica.getId(), targetServerId});
        return replica;
    }

    // ========== Private helpers ==========

    private List<Store> findAllByOwner(String owner) {
        return em.createNamedQuery("Store.findByOwner", Store.class)
            .setParameter("owner", owner)
            .getResultList();  // getResultList() pour supporter plusieurs stores
    }

    private String lookup(String name) {
        try {
            return topology.lookup(name);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error looking up store in topology: {0}", new Object[]{name, e});
            return null;
        }
    }

}
