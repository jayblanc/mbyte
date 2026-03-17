package fr.jayblanc.mbyte.manager.core.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

@Entity
@NamedQueries({
    @NamedQuery(name = "Store.findByOwner", query = "SELECT s FROM Store s WHERE s.owner = :owner ORDER BY s.creationDate"),
    @NamedQuery(name = "Store.findByOwnerAndName", query = "SELECT s FROM Store s WHERE s.owner = :owner AND s.name = :name"),
    @NamedQuery(name = "Store.countByOwner", query = "SELECT COUNT(s) FROM Store s WHERE s.owner = :owner"),
    @NamedQuery(name = "Store.findByServerId", query = "SELECT s FROM Store s WHERE s.serverId = :serverId"),
    @NamedQuery(name = "Store.findByOwnerAndNameAndServerId", query = "SELECT s FROM Store s WHERE s.owner = :owner AND s.name = :name AND s.serverId = :serverId")
})
@Table(
    indexes = {
        @Index(name = "stores_idx", columnList = "owner"),
        @Index(name = "stores_owner_name_idx", columnList = "owner, name"),
        @Index(name = "idx_store_serverid", columnList = "serverId")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_store_owner_name_server", columnNames = {"owner", "name", "serverId"})
    }
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Store {

    @Id
    private String id;
    private String type;
    private String owner;
    private String name;
    private long creationDate;
    private float usage;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Lob
    private String log;
    @Transient
    private String location;
    private String serverId;

    public Store() {
    }

    public Store(String id, String type, String owner, String name) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.name = name;
        this.creationDate = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public float getUsage() {
        return usage;
    }

    public void setUsage(float usage) {
        this.usage = usage;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public enum Status {
        PENDING,
        AVAILABLE,
        LOST
    }
}
