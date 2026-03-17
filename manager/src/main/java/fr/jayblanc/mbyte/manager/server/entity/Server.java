package fr.jayblanc.mbyte.manager.server.entity;

/**
 * Represents a Docker server that can host MByte stores.
 * Servers are configured in application.properties and managed by the ServerRegistry.
 */
public class Server {

    private String id;
    private String name;
    private String dockerHost;
    private String workdirHost;
    private String workdirLocal;
    private String traefikNetwork;
    private boolean enabled;
    private int capacity;
    private int priority;
    private ServerStatus status;

    public enum ServerStatus {
        ONLINE,
        OFFLINE,
        DEGRADED,
        UNKNOWN
    }

    public Server() {
        this.status = ServerStatus.UNKNOWN;
    }

    public Server(String id, String name, String dockerHost) {
        this.id = id;
        this.name = name;
        this.dockerHost = dockerHost;
        this.status = ServerStatus.UNKNOWN;
        this.enabled = true;
        this.capacity = 0;
        this.priority = 1;
        this.traefikNetwork = "mbyte.net";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDockerHost() {
        return dockerHost;
    }

    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    public String getWorkdirHost() {
        return workdirHost;
    }

    public void setWorkdirHost(String workdirHost) {
        this.workdirHost = workdirHost;
    }

    public String getWorkdirLocal() {
        return workdirLocal;
    }

    public void setWorkdirLocal(String workdirLocal) {
        this.workdirLocal = workdirLocal;
    }

    public String getTraefikNetwork() {
        return traefikNetwork;
    }

    public void setTraefikNetwork(String traefikNetwork) {
        this.traefikNetwork = traefikNetwork;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public ServerStatus getStatus() {
        return status;
    }

    public void setStatus(ServerStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Server{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", dockerHost='" + dockerHost + '\'' +
                ", status=" + status +
                ", enabled=" + enabled +
                '}';
    }
}
