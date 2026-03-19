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

import fr.jayblanc.mbyte.store.auth.AuthenticationService;
import fr.jayblanc.mbyte.store.data.DataStore;
import fr.jayblanc.mbyte.store.data.exception.DataNotFoundException;
import fr.jayblanc.mbyte.store.data.exception.DataStoreException;
import fr.jayblanc.mbyte.store.files.entity.Node;
import fr.jayblanc.mbyte.store.files.exceptions.*;
import fr.jayblanc.mbyte.store.index.IndexableContent;
import fr.jayblanc.mbyte.store.index.IndexableContentProvider;
import fr.jayblanc.mbyte.store.metrics.GenerateMetric;
import fr.jayblanc.mbyte.store.metrics.MetricsSource;
import fr.jayblanc.mbyte.store.notification.NotificationService;
import fr.jayblanc.mbyte.store.notification.NotificationServiceException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@MetricsSource
@ApplicationScoped
public class FileServiceBean implements FileService, IndexableContentProvider {

    private static final Logger LOGGER = Logger.getLogger(FileServiceBean.class.getName());
    private static boolean initialized = false;

    @Inject DataStore datastore;
    @Inject NotificationService notification;
    @Inject AuthenticationService auth;
    @Inject fr.jayblanc.mbyte.store.index.IndexStoreConfig indexConfig;
    @Inject EntityManager em;

    public FileServiceBean() {
    }

    @PostConstruct
    public synchronized void init() {
        if (!initialized) {
            LOGGER.log(Level.INFO, "Initialising file service");
            try {
                boolean bootstrap = false;
                try {
                    systemLoadNode(ROOT_NODE_ID);
                } catch (NodeNotFoundException e ) {
                    bootstrap = true;
                }
                if ( bootstrap ) {
                    LOGGER.log(Level.INFO, "Root node does not exists, applying bootstrap");
                    Node root = new Node(Node.Type.TREE, "", ROOT_NODE_ID, "root");
                    em.persist(root);
                    LOGGER.log(Level.INFO, "Bootstrap done, root node exists now.");
                }
            } catch (PersistenceException e) {
                throw new RuntimeException(e);
            }
            initialized = true;
        }
    }

    @Override
    public List<Node> list(String parent) throws NodeNotFoundException {
        LOGGER.log(Level.INFO, "Listing children for parent: " + parent);
        Node pnode = this.loadNode(parent);
        List<Node> nodes = em.createNamedQuery("Node.findAllChildren", Node.class).setParameter("parent", pnode.getId()).getResultList();
        return nodes;
    }


    @Override
    public List<Node> path(String id) throws NodeNotFoundException {
        LOGGER.log(Level.FINE, "Get path for node with id: " + id);
        String pid = (id == null || id.isEmpty()) ? ROOT_NODE_ID:id;
        List<Node> path = new ArrayList<>();
        while (pid != null && !pid.isEmpty()) {
            Node current = loadNode(pid);
            path.add(current);
            pid = current.getParent();
        }
        Collections.reverse(path);
        LOGGER.log(Level.FINE, "path: " + path.stream().map(Node::getName).collect(Collectors.joining(" > ")));
        //LOGGER.log(Level.INFO, "Full path String : " + this.getFullPath(path));
        return path;
    }


    public String getFullPath(List<Node> nodesPath){
        return nodesPath.stream().map(Node::getName).collect(Collectors.joining("/"));
    }

    public List<Node> findAll() throws NodeNotFoundException {
        List<Node> nodes = em.createNamedQuery("Node.findAll", Node.class).getResultList();
        LOGGER.log(Level.INFO,"Test recup nb nodes: " + nodes.size());
        for (Node node : nodes){
             LOGGER.log(Level.INFO, "Full path du fichier: "+this.getFullPath(this.path(node.getId())));
        }
        return nodes;
    }


    @Override
    public Node get(String id) throws NodeNotFoundException {
        LOGGER.log(Level.INFO, "Getting node with id: " + id);
        return this.loadNode(id);
    }


    @Override
    @GenerateMetric(key = "download", type = GenerateMetric.Type.INCREMENT)
    public InputStream getContent(String id) throws NodeNotFoundException, NodeTypeException, DataNotFoundException, DataStoreException {
        LOGGER.log(Level.INFO, "Getting content for node with id: " + id);
        Node node = this.loadNode(id);
        if (!node.getType().equals(Node.Type.BLOB)) {
            throw new NodeTypeException("only node of type BLOB have content");
        }
        return datastore.get(node.getContent());
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public String add(String parent, String name) throws NodeNotFoundException, NodeAlreadyExistsException, NodeTypeException,
            NodePersistenceException, NotificationServiceException {
        LOGGER.log(Level.INFO, "Adding TREE node with name: " + name + " for parent: " + parent);
        Node pnode = this.loadNode(parent);
        if (!pnode.isFolder()) {
            throw new NodeTypeException("Parent must be a node of type TREE");
        }
        List<Node> nodes = em.createNamedQuery("Node.findChildrenForName", Node.class).setParameter("parent", pnode.getId()).setParameter("name", name).getResultList();
        if (!nodes.isEmpty()) {
            throw new NodeAlreadyExistsException("A node with name: " + name + " already exists in tree with id: " + pnode.getId());
        }
        Node node = new Node(Node.Type.TREE, pnode.getId(), UUID.randomUUID().toString(), name);
        node.setMimetype(TREE_NODE_MIMETYPE);
        em.persist(node);
        pnode.setSize(pnode.getSize()+1);
        pnode.setModification(node.getModification());
        notification.notify("folder.create", node.getId());
        notification.notify("folder.update", pnode.getId());
        return node.getId();
    }

    @Override
    @GenerateMetric(key = "upload", type = GenerateMetric.Type.INCREMENT)
    @Transactional(Transactional.TxType.REQUIRED)
    public String add(String parent, String name, InputStream content) throws NodeNotFoundException, NodeAlreadyExistsException, NodeTypeException, DataStoreException, DataNotFoundException, NodePersistenceException, NotificationServiceException {
        LOGGER.log(Level.INFO, "Adding BLOB node with name: " + name + " to parent: " + parent);
        Node pnode = this.loadNode(parent);
        if (!pnode.isFolder()) {
            throw new NodeTypeException("Parent must be a node of type TREE");
        }
        List<Node> nodes = em.createNamedQuery("Node.findChildrenForName", Node.class).setParameter("parent", pnode.getId()).setParameter("name", name).getResultList();
        if (!nodes.isEmpty()) {
            throw new NodeAlreadyExistsException("A node with name: " + name + " already exists in tree with id: " + pnode.getId());
        }
        String cid = datastore.put(content);
        Node node = new Node(Node.Type.BLOB, pnode.getId(), UUID.randomUUID().toString(), name);
        node.setContent(cid);
        node.setSize(datastore.size(cid));
        node.setMimetype(datastore.type(cid, name));
        em.persist(node);
        pnode.setSize(pnode.getSize()+1);
        pnode.setModification(node.getModification());
        notification.notify("file.create", node.getId());
        notification.notify("folder.update", pnode.getId());
        return node.getId();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void remove(String parent, String name) throws NodeNotFoundException, NodeNotEmptyException, NodeTypeException, DataStoreException, NodePersistenceException, NotificationServiceException {
        LOGGER.log(Level.FINE, "Remove node with name: " + name + " and parent: " + parent);
        Node pnode = this.loadNode(parent);
        if (!pnode.isFolder()) {
            throw new NodeTypeException("Parent must be a node of type TREE");
        }
        Node node = em.createNamedQuery("Node.findChildrenForName", Node.class).setParameter("parent", pnode.getId()).setParameter("name", name).getSingleResult();
        if (node == null) {
            throw new NodeNotFoundException("A node with name: " + name + " does not exists in tree with id: " + pnode.getId());
        }
        int children = em.createNamedQuery("Node.countChildren", Integer.class).setParameter("parent", pnode.getId()).getSingleResult();
        if (children > 0) {
            throw new NodeNotEmptyException("The node with name: " + name + " is not empty");
        }
        String eventType = "folder.remove";
        if (!node.isFolder()) {
            datastore.delete(node.getContent());
            eventType = "file.remove";
        }
        em.remove(node);
        pnode.setSize(pnode.getSize()-1);
        pnode.setModification(System.currentTimeMillis());
        notification.notify(eventType, node.getId());
        notification.notify("folder.update", pnode.getId());
    }

    //INTERNAL OPERATIONS

    private Node loadNode(String id) throws NodeNotFoundException {
        return systemLoadNode(id);
    }

    private Node systemLoadNode(String id) throws NodeNotFoundException {
        String pid = (id == null || id.isEmpty()) ? ROOT_NODE_ID:id;
        Node node = em.find(Node.class, pid);
        if (node == null) {
            throw new NodeNotFoundException("unable to find a node with id: " + pid);
        }
        return node;
    }

    private Node loadNodeWithLock(String id) throws NodeNotFoundException {
        String pid = (id == null || id.isEmpty()) ? ROOT_NODE_ID:id;
        Node node = em.find(Node.class, pid, LockModeType.PESSIMISTIC_WRITE);
        if (node == null) {
            throw new NodeNotFoundException("unable to find a node with id: " + pid);
        }
        return node;
    }

    @Override
    public IndexableContent getIndexableContent(String id) {
        IndexableContent content = new IndexableContent();
        content.setIdentifier(id);
        content.setType("node");
        content.setScope(IndexableContent.Scope.PRIVATE);
        content.setStoreId(indexConfig.typesense().storeId());
        content.setContent("");
        try {
            Node node = systemLoadNode(id);
            content.setName(node.getName());
            content.setMimetype(node.getMimetype());
            content.setNodeType(node.getType().name());
            content.setParent(node.getParent());
            content.setModifiedAt(node.getModification());
            if (node.isFolder()) {
                content.setContent(node.getName() + " " + node.getMimetype());
            } else {
                String extracted = datastore.extract(node.getContent(), node.getName(), node.getMimetype());
                if ((extracted == null || extracted.isBlank()) && isTextLike(node.getMimetype())) {
                    extracted = extractRawTextPreview(node.getContent());
                }
                content.setContent(node.getName() + " " + node.getMimetype() + " " + (extracted == null ? "" : extracted));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while extracting indexable content for node with id: " + id);
        }
        return content;
    }

    private boolean isTextLike(String mimetype) {
        if (mimetype == null) {
            return false;
        }
        String mt = mimetype.toLowerCase();
        return mt.startsWith("text/")
                || mt.contains("xml")
                || mt.contains("json")
                || mt.contains("yaml")
                || mt.contains("csv")
                || mt.contains("javascript");
    }

    private String extractRawTextPreview(String key) {
        try (InputStream is = datastore.get(key)) {
            byte[] bytes = is.readAllBytes();
            String raw = new String(bytes, StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ")
                    .trim();
            if (raw.length() > 20000) {
                return raw.substring(0, 20000);
            }
            return raw;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Fallback raw text extraction failed for key: " + key, e);
            return "";
        }
    }
}
