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
package fr.jayblanc.mbyte.store.index;

import fr.jayblanc.mbyte.store.files.FileServiceBean;
import fr.jayblanc.mbyte.store.files.entity.Node;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class IndexStoreBootstrapBean {

    private static final Logger LOGGER = Logger.getLogger(IndexStoreBootstrapBean.class.getName());

    @Inject IndexStoreConfig config;
    @Inject EntityManager em;
    @Inject FileServiceBean files;
    @Inject IndexStoreService index;

    @PostConstruct
    public void reindexIfEnabled() {
        if (!config.bootstrap().reindex()) {
            LOGGER.log(Level.INFO, "Typesense bootstrap reindex disabled");
            return;
        }
        List<Node> nodes = em.createNamedQuery("Node.findAll", Node.class).getResultList();
        try {
            index.clearStoreDocuments();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to purge existing Typesense documents before reindex", e);
        }
        LOGGER.log(Level.INFO, "Reindexing {0} node(s) into Typesense", nodes.size());
        int indexed = 0;
        for (Node node : nodes) {
            try {
                index.index(files.getIndexableContent(node.getId()));
                indexed++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to reindex node " + node.getId(), e);
            }
        }
        LOGGER.log(Level.INFO, "Typesense reindex completed, indexed {0}/{1} node(s)", new Object[]{indexed, nodes.size()});
    }
}
