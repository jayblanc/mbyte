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
package fr.jayblanc.mbyte.store.search;

import fr.jayblanc.mbyte.store.auth.AuthenticationService;
import fr.jayblanc.mbyte.store.index.IndexStoreConversationResult;
import fr.jayblanc.mbyte.store.index.IndexStoreException;
import fr.jayblanc.mbyte.store.index.IndexStoreResult;
import fr.jayblanc.mbyte.store.index.IndexStoreService;
import fr.jayblanc.mbyte.store.index.IndexableContent;
import fr.jayblanc.mbyte.store.metrics.GenerateMetric;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class SearchServiceBean implements SearchService {

    private static final Logger LOGGER = Logger.getLogger(SearchServiceBean.class.getName());

    @Inject IndexStoreService index;
    @Inject AuthenticationService auth;

    @Override
    @GenerateMetric(key = "search", type = GenerateMetric.Type.INCREMENT)
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<SearchResult> search(String query) throws SearchServiceException {
        LOGGER.log(Level.FINE, "Searching results for query: " + query);
        try {
            String scope = (auth.getConnectedProfile().isOwner())? IndexableContent.Scope.PRIVATE.name(): IndexableContent.Scope.PUBLIC.name();
            List<IndexStoreResult> results = index.search(scope, query);
            return results.stream().map(res -> {
                SearchResult result = SearchResult.fromIndexStoreResult(res);
                return result;
            }).collect(Collectors.toList());
        } catch (IndexStoreException e ) {
            throw new SearchServiceException("Error while searching query", e);
        }
    }

    @Override
    @GenerateMetric(key = "search.conversation", type = GenerateMetric.Type.INCREMENT)
    @Transactional(Transactional.TxType.SUPPORTS)
    public SearchConversationResult converse(String query, String conversationId) throws SearchServiceException {
        LOGGER.log(Level.FINE, "Conversational search for query: " + query);
        try {
            String scope = (auth.getConnectedProfile().isOwner()) ? IndexableContent.Scope.PRIVATE.name() : IndexableContent.Scope.PUBLIC.name();
            IndexStoreConversationResult result = index.converse(scope, query, conversationId);
            return SearchConversationResult.fromIndexStoreResult(result);
        } catch (IndexStoreException e) {
            throw new SearchServiceException("Error while running conversational search", e);
        }
    }

    @Override
    @GenerateMetric(key = "search.conversation.stream", type = GenerateMetric.Type.INCREMENT)
    @Transactional(Transactional.TxType.SUPPORTS)
    public InputStream streamConversation(String query, String conversationId) throws SearchServiceException {
        LOGGER.log(Level.FINE, "Conversational stream for query: " + query);
        try {
            String scope = (auth.getConnectedProfile().isOwner()) ? IndexableContent.Scope.PRIVATE.name() : IndexableContent.Scope.PUBLIC.name();
            return index.converseStream(scope, query, conversationId);
        } catch (IndexStoreException e) {
            throw new SearchServiceException("Error while streaming conversational search", e);
        }
    }

}
