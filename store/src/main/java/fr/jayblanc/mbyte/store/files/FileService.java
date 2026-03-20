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
package fr.jayblanc.mbyte.store.files;

import fr.jayblanc.mbyte.store.data.exception.DataNotFoundException;
import fr.jayblanc.mbyte.store.data.exception.DataStoreException;
import fr.jayblanc.mbyte.store.files.entity.Node;
import fr.jayblanc.mbyte.store.files.exceptions.*;
import fr.jayblanc.mbyte.store.notification.NotificationServiceException;

import java.io.InputStream;
import java.util.List;

public interface FileService {

    String ROOT_NODE_ID = "root";
    String TREE_NODE_MIMETYPE = "application/fs-folder";

    List<Node> list(String id) throws NodeNotFoundException;

    List<Node> path(String id) throws NodeNotFoundException;

    Node get(String id) throws NodeNotFoundException;

    InputStream getContent(String id) throws NodeNotFoundException, NodeTypeException, DataNotFoundException, DataStoreException;

    String add(String parent, String name) throws NodeNotFoundException, NodeAlreadyExistsException, NodeTypeException,
            NodePersistenceException, NotificationServiceException;

    String add(String parent, String name, InputStream content) throws NodeNotFoundException, NodeAlreadyExistsException, NodeTypeException, DataStoreException, DataNotFoundException, NodePersistenceException, NotificationServiceException;

    void remove(String parent, String name) throws NodeNotFoundException, NodeNotEmptyException, NodeTypeException, DataStoreException, NodePersistenceException, NotificationServiceException;

    void flush();

    String getFullPath(List<Node> nodesPath);

    List<Node> findAll() throws NodeNotFoundException;

}
