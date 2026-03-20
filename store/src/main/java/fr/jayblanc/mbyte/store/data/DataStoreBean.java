/*
 * Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.jayblanc.mbyte.store.data;

import fr.jayblanc.mbyte.store.data.exception.DataNotFoundException;
import fr.jayblanc.mbyte.store.data.exception.DataStoreException;
import fr.jayblanc.mbyte.store.data.hash.HashedFilterInputStream;
import io.quarkus.runtime.Startup;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jerome Blanchard
 */
@Singleton
public class DataStoreBean implements DataStore {

    private static final Logger LOGGER = Logger.getLogger(DataStore.class.getName());

    @Inject
    DataStoreConfig config;

    private Path base;
    private Tika tika;

    public DataStoreBean() {
    }

    @Startup
    public void init() {
        this.base = Paths.get(config.home());
        LOGGER.log(Level.FINEST, "Initializing service with base folder: " + base);
        try {
            Files.createDirectories(base);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to initialize data store", e);
        }
        this.tika = new Tika();
    }

    @Override
    public boolean exists(String key) {
        Path file = Paths.get(base.toString(), key);
        return Files.exists(file);
    }

    @Override
    public String put(InputStream is) throws DataStoreException {
        String tmpkey = UUID.randomUUID().toString();
        Path tmpfile = Paths.get(base.toString(), tmpkey);
        try (HashedFilterInputStream his = HashedFilterInputStream.SHA256(is)) {
            Files.copy(his, tmpfile, StandardCopyOption.REPLACE_EXISTING);
            String key = his.getHash();
            Path file = Paths.get(base.toString(), key);
            if ( !Files.exists(file) ) {
                Files.move(tmpfile, file);
            } else {
                Files.delete(tmpfile);
            }
            return key;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new DataStoreException("unexpected error during stream copy", e);
        }
    }

    @Override
    public InputStream get(String key) throws DataStoreException, DataNotFoundException {
        Path file = Paths.get(base.toString(), key);
        if ( !Files.exists(file) ) {
            throw new DataNotFoundException("file not found in storage for key: " + key);
        }
        try {
            return Files.newInputStream(file, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new DataStoreException("unexpected error while opening stream", e);
        }
    }

    @Override
    public String type(String key, String name) throws DataNotFoundException {
        LOGGER.log(Level.FINE, "Extract type for key: " + key);
        Path file = Paths.get(base.toString(), key);
        if ( !Files.exists(file) ) {
            throw new DataNotFoundException("file not found in storage for key: " + key);
        }
        String mimetype = MediaType.APPLICATION_OCTET_STREAM;
        try (InputStream stream = Files.newInputStream(file)) {
            mimetype = tika.detect(stream, name);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to detect mimetype: " + e.getMessage(), e);
        }
        return mimetype;
    }

    @Override
    public long size(String key) throws DataStoreException, DataNotFoundException {
        Path file = Paths.get(base.toString(), key);
        if ( !Files.exists(file) ) {
            throw new DataNotFoundException("file not found in storage for key: " + key);
        }
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new DataStoreException("unexpected error while getting stream size", e);
        }
    }

    @Override
    public String extract(String key, String name, String type) throws DataStoreException, DataNotFoundException {
        LOGGER.log(Level.FINE, "Extract text for key: " + key);
        Path file = Paths.get(base.toString(), key);
        if ( !Files.exists(file) ) {
            throw new DataNotFoundException("file not found in storage");
        }
        try (InputStream stream = Files.newInputStream(file)) {
            BodyContentHandler handler = new BodyContentHandler();
            AutoDetectParser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            metadata.set(Metadata.CONTENT_TYPE, type);
            parser.parse(stream, handler, metadata);
            return handler.toString();
        } catch (IOException | SAXException | TikaException e) {
            throw new DataStoreException("unexpected error while opening stream", e);
        }
    }

    @Override
    public void delete(String key) throws DataStoreException {
        Path file = Paths.get(base.toString(), key);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new DataStoreException("unable to delete file from storage", e);
        }
    }


}
