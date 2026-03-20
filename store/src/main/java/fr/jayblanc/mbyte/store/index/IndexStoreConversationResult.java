package fr.jayblanc.mbyte.store.index;

import java.util.ArrayList;
import java.util.List;

public class IndexStoreConversationResult {

    private String answer;
    private String conversationId;
    private String query;
    private List<IndexStoreResult> results = new ArrayList<>();

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

    public List<IndexStoreResult> getResults() {
        return results;
    }

    public void setResults(List<IndexStoreResult> results) {
        this.results = results;
    }
}
