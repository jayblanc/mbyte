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
