package fr.jayblanc.mbyte.manager.core;

import fr.jayblanc.mbyte.manager.core.entity.Store;
import java.util.List;

public interface CoreService {

    // Existant (rétrocompatibilité)
    Store createStore(String name);
    Store getConnectedUserStore() throws StoreNotFoundException, CoreServiceException;

    // Multi-Server Support
    Store createStore(String name, String preferredServerId);

    // Multi-Store Support
    List<Store> getUserStores();
    Store getStoreById(String storeId) throws StoreNotFoundException;
    void deleteStore(String storeId) throws StoreNotFoundException, CoreServiceException;
    Store renameStore(String storeId, String newName) throws StoreNotFoundException;
    long getStoreCount();

    // Multi-Server Replication
    Store replicateStore(String storeId, String targetServerId) throws StoreNotFoundException, CoreServiceException;

}
