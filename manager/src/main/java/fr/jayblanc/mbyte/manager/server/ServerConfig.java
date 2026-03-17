package fr.jayblanc.mbyte.manager.server;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Map;

/**
 * Configuration interface for multi-server deployment.
 * Servers are defined in application.properties with the prefix manager.servers
 */
@ConfigMapping(prefix = "manager.servers")
public interface ServerConfig {

    /**
     * Server selection strategy: round-robin, least-loaded
     */
    @WithDefault("round-robin")
    String selectionStrategy();

    /**
     * Map of server configurations keyed by server ID
     */
    Map<String, ServerDefinition> servers();

    interface ServerDefinition {
        /**
         * Human-readable name for the server
         */
        String name();

        /**
         * Docker daemon connection URL
         * Examples: unix:///var/run/docker.sock or tcp://192.168.1.100:2375
         */
        String dockerHost();

        /**
         * Host path for store volumes on this server
         */
        String workdirHost();

        /**
         * Local path for volumes (from manager's perspective)
         */
        @WithDefault("")
        String workdirLocal();

        /**
         * Traefik network name on this server
         */
        @WithDefault("mbyte.net")
        String traefikNetwork();

        /**
         * Whether this server is available for new deployments
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Maximum number of stores this server can host (0 = unlimited)
         */
        @WithDefault("0")
        int capacity();

        /**
         * Priority for selection (higher = preferred)
         */
        @WithDefault("1")
        int priority();
    }
}
