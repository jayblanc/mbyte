package fr.jayblanc.mbyte.manager.store;

import fr.jayblanc.mbyte.manager.server.entity.Server;

import java.util.List;

/**
 * Interface for store deployment providers.
 * Supports multi-server deployment through server-aware methods.
 */
public interface StoreProvider {

    /**
     * Get the provider name (used for configuration matching)
     */
    String name();

    /**
     * List all stores across all servers
     */
    List<String> listAllStores() throws StoreProviderException;

    /**
     * List stores on a specific server
     */
    List<String> listStores(Server server) throws StoreProviderException;

    /**
     * Create a store using the default/first available server (backwards compatible)
     */
    String createStore(String id, String owner, String name) throws StoreProviderException;

    /**
     * Create a store on a specific server
     */
    String createStore(String id, String owner, String name, Server server) throws StoreProviderException;

    /**
     * Destroy a store (searches across all servers)
     */
    String destroyStore(String id) throws StoreProviderException;

    /**
     * Destroy a store on a specific server
     */
    String destroyStore(String id, Server server) throws StoreProviderException;
}
