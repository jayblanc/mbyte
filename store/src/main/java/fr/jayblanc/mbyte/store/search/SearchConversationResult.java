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

import fr.jayblanc.mbyte.store.index.IndexStoreConversationResult;

import java.util.ArrayList;
import java.util.List;

public class SearchConversationResult {

    private String answer;
    private String conversationId;
    private String query;
    private List<SearchResult> results = new ArrayList<>();

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<SearchResult> getResults() {
        return results;
    }

    public void setResults(List<SearchResult> results) {
        this.results = results;
    }

    public static SearchConversationResult fromIndexStoreResult(IndexStoreConversationResult result) {
        SearchConversationResult searchResult = new SearchConversationResult();
        searchResult.setAnswer(result.getAnswer());
        searchResult.setConversationId(result.getConversationId());
        searchResult.setQuery(result.getQuery());
        searchResult.setResults(result.getResults().stream().map(SearchResult::fromIndexStoreResult).toList());
        return searchResult;
    }
}
