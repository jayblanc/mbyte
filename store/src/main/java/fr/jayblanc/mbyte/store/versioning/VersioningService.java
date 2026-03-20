package fr.jayblanc.mbyte.store.versioning;

import fr.jayblanc.mbyte.store.versioning.entity.FileVersion;

import java.io.InputStream;
import java.util.List;

public interface VersioningService {

    FileVersion createVersion(String nodeId, String nodeName, InputStream content,
                              String mimetype, long size, String author, String comment);

    List<FileVersion> getHistory(String nodeId);

    FileVersion getVersion(String versionId);

    InputStream getVersionContent(String versionId);

    void deleteVersion(String versionId);

    long getVersionCount(String nodeId);
}
