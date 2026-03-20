package fr.jayblanc.mbyte.store.versioning.entity;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "file_version")
@NamedQueries({
        @NamedQuery(name = "FileVersion.findByNodeId",
                query = "SELECT v FROM FileVersion v WHERE v.nodeId = :nodeId ORDER BY v.createdAt DESC"),
        @NamedQuery(name = "FileVersion.countByNodeId",
                query = "SELECT COUNT(v) FROM FileVersion v WHERE v.nodeId = :nodeId"),
})
public class FileVersion implements Serializable {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "node_id", length = 50, nullable = false)
    private String nodeId;

    @Column(name = "node_name", length = 255)
    private String nodeName;

    @Column(length = 255)
    private String content;

    @Column(length = 50)
    private String mimetype;

    private long size;

    @Column(length = 250)
    private String author;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "version_number", nullable = false)
    private long versionNumber;

    public FileVersion() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMimetype() { return mimetype; }
    public void setMimetype(String mimetype) { this.mimetype = mimetype; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getVersionNumber() { return versionNumber; }
    public void setVersionNumber(long versionNumber) { this.versionNumber = versionNumber; }
}
