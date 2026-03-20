package fr.jayblanc.mbyte.store.api.dto;

import fr.jayblanc.mbyte.store.versioning.entity.FileVersion;

import java.util.Date;

public class FileVersionDto {

    private String id;
    private String nodeId;
    private String nodeName;
    private String author;
    private String comment;
    private String mimetype;
    private long size;
    private long versionNumber;
    private Date createdAt;

    public static FileVersionDto fromEntity(FileVersion v) {
        FileVersionDto dto = new FileVersionDto();
        dto.id = v.getId();
        dto.nodeId = v.getNodeId();
        dto.nodeName = v.getNodeName();
        dto.author = v.getAuthor();
        dto.comment = v.getComment();
        dto.mimetype = v.getMimetype();
        dto.size = v.getSize();
        dto.versionNumber = v.getVersionNumber();
        dto.createdAt = new Date(v.getCreatedAt());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getMimetype() { return mimetype; }
    public void setMimetype(String mimetype) { this.mimetype = mimetype; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public long getVersionNumber() { return versionNumber; }
    public void setVersionNumber(long versionNumber) { this.versionNumber = versionNumber; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
