package fr.jayblanc.mbyte.store.api.dto;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.providers.multipart.PartType;

import java.io.InputStream;

public class VersionCreateDto {

    @FormParam("name")
    @PartType(MediaType.TEXT_PLAIN)
    private String name;

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private InputStream file;

    @FormParam("mimetype")
    @PartType(MediaType.TEXT_PLAIN)
    private String mimetype;

    @FormParam("size")
    @PartType(MediaType.TEXT_PLAIN)
    private String size;

    @FormParam("comment")
    @PartType(MediaType.TEXT_PLAIN)
    private String comment;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public InputStream getFile() { return file; }
    public void setFile(InputStream file) { this.file = file; }
    public String getMimetype() { return mimetype; }
    public void setMimetype(String mimetype) { this.mimetype = mimetype; }
    public long getSize() {
        try { return Long.parseLong(size); } catch (Exception e) { return 0; }
    }
    public void setSize(String size) { this.size = size; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
