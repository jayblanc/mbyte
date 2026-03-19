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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class IndexStoreServiceBean implements IndexStoreService {

    private static final Logger LOGGER = Logger.getLogger(IndexStoreServiceBean.class.getName());
    private static final Duration CONVERSATION_RETRY_INTERVAL = Duration.ofSeconds(15);
    private static final Duration CONVERSATION_REQUEST_TIMEOUT = Duration.ofSeconds(90);

    @Inject IndexStoreConfig config;
    @Inject ObjectMapper mapper;

    private HttpClient client;
    private URI baseUri;
    private volatile boolean conversationReady;
    private volatile Instant lastConversationInitAttempt;
    private final Object conversationInitLock = new Object();

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "Initializing Typesense index service");
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        baseUri = URI.create(String.format("%s://%s:%d", config.typesense().protocol(), config.typesense().host(), config.typesense().port()));
        conversationReady = false;
        lastConversationInitAttempt = Instant.EPOCH;
        try {
            ensureCollection();
            if (config.typesense().conversation().enabled()) {
                ensureConversationReady();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize Typesense collection", e);
            throw new RuntimeException("Unable to initialize Typesense collection", e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public void clearStoreDocuments() throws IndexStoreException {
        String filter = IndexStoreDocumentBuilder.STORE_ID_FIELD + ":=" + config.typesense().storeId();
        LOGGER.log(Level.INFO, "Purging Typesense documents for store_id={0}", config.typesense().storeId());
        try {
            HttpRequest request = baseRequest(
                    "/collections/" + encode(config.typesense().collection()) + "/documents?filter_by=" + encode(filter))
                    .DELETE()
                    .build();
            sendExpectSuccess(request, "purge store documents for " + config.typesense().storeId());
        } catch (Exception e) {
            throw new IndexStoreException("Can't purge documents for store " + config.typesense().storeId(), e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public void index(IndexableContent object) throws IndexStoreException {
        LOGGER.log(Level.INFO, "Indexing object in Typesense: {0}", object.getIdentifier());
        try {
            String payload = mapper.writeValueAsString(IndexStoreDocumentBuilder.buildDocument(object));
            HttpRequest request = baseRequest("/collections/" + encode(config.typesense().collection()) + "/documents?action=upsert")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            sendExpectSuccess(request, "upsert document " + object.getIdentifier());
        } catch (Exception e) {
            throw new IndexStoreException("Can't index an object", e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public void remove(String identifier) throws IndexStoreException {
        LOGGER.log(Level.INFO, "Removing document from Typesense: {0}", identifier);
        try {
            HttpRequest request = baseRequest("/collections/" + encode(config.typesense().collection()) + "/documents/" + encode(identifier))
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 404) {
                throw new IndexStoreException("Can't remove object " + identifier + " from index, status=" + response.statusCode() + " body=" + response.body());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IndexStoreException("Can't remove object " + identifier + " from index", e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<IndexStoreResult> search(String scope, String queryString) throws IndexStoreException {
        LOGGER.log(Level.INFO, "Searching query in Typesense: {0}", queryString);
        try {
            String path = "/collections/" + encode(config.typesense().collection()) + "/documents/search"
                    + "?q=" + encode(queryString == null || queryString.isBlank() ? "*" : queryString)
                    + "&query_by=" + encode(String.join(",", IndexStoreDocumentBuilder.CONTENT_FIELD, IndexStoreDocumentBuilder.NAME_FIELD, IndexStoreDocumentBuilder.MIMETYPE_FIELD))
                    + "&highlight_fields=" + encode(String.join(",", IndexStoreDocumentBuilder.CONTENT_FIELD, IndexStoreDocumentBuilder.NAME_FIELD))
                    + "&filter_by=" + encode(IndexStoreDocumentBuilder.STORE_ID_FIELD + ":=" + config.typesense().storeId() + " && "
                    + IndexStoreDocumentBuilder.SCOPE_FIELD + ":=" + scope)
                    + "&per_page=100";
            HttpRequest request = baseRequest(path).GET().build();
            HttpResponse<String> response = sendExpectSuccess(request, "search query " + queryString);
            JsonNode root = mapper.readTree(response.body());
            return parseHits(root.path("hits"));
        } catch (Exception e) {
            throw new IndexStoreException("Can't search in index using '" + queryString + "'", e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public IndexStoreConversationResult converse(String scope, String query, String conversationId) throws IndexStoreException {
        if (!ensureConversationReady()) {
            throw new IndexStoreException("Typesense conversational search is temporarily unavailable");
        }
        LOGGER.log(Level.INFO, "Conversational search query in Typesense: {0}", query);
        try {
            String queryString = query == null || query.isBlank() ? "*" : query;
            String path = "/multi_search"
                    + "?q=" + encode(queryString)
                    + "&conversation=true"
                    + "&conversation_model_id=" + encode(config.typesense().conversation().modelId());
            if (conversationId != null && !conversationId.isBlank()) {
                path += "&conversation_id=" + encode(conversationId);
            }

            HttpRequest request = baseRequest(path, CONVERSATION_REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(buildConversationPayload(scope))))
                    .build();
            HttpResponse<String> response = sendExpectSuccess(request, "conversational search query " + queryString);
            JsonNode root = mapper.readTree(response.body());
            JsonNode conversation = root.path("conversation");
            JsonNode results = root.path("results").isArray() && !root.path("results").isEmpty()
                    ? root.path("results").get(0)
                    : mapper.createObjectNode();

            IndexStoreConversationResult result = new IndexStoreConversationResult();
            result.setAnswer(conversation.path("answer").asText(""));
            result.setConversationId(conversation.path("conversation_id").asText(""));
            result.setQuery(conversation.path("query").asText(queryString));
            result.setResults(parseHits(results.path("hits")));
            return result;
        } catch (Exception e) {
            throw new IndexStoreException("Can't run conversational search using '" + query + "'", e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public InputStream converseStream(String scope, String query, String conversationId) throws IndexStoreException {
        if (!ensureConversationReady()) {
            throw new IndexStoreException("Typesense conversational search is temporarily unavailable");
        }
        LOGGER.log(Level.INFO, "Streaming conversational query in Typesense: {0}", query);
        try {
            String queryString = query == null || query.isBlank() ? "*" : query;
            String path = "/multi_search"
                    + "?q=" + encode(queryString)
                    + "&conversation=true"
                    + "&conversation_stream=true"
                    + "&conversation_model_id=" + encode(config.typesense().conversation().modelId());
            if (conversationId != null && !conversationId.isBlank()) {
                path += "&conversation_id=" + encode(conversationId);
            }
            HttpRequest request = baseRequest(path, CONVERSATION_REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(buildConversationPayload(scope))))
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                String body = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IndexStoreException("Unable to stream conversational search, status=" + response.statusCode() + " body=" + body);
            }
            return response.body();
        } catch (Exception e) {
            throw new IndexStoreException("Can't stream conversational search using '" + query + "'", e);
        }
    }

    private void ensureCollection() throws IOException, InterruptedException, IndexStoreException {
        String collection = config.typesense().collection();
        HttpRequest get = baseRequest("/collections/" + encode(collection)).GET().build();
        HttpResponse<String> existing = client.send(get, HttpResponse.BodyHandlers.ofString());
        if (existing.statusCode() == 200) {
            LOGGER.log(Level.INFO, "Typesense collection already exists: {0}", collection);
            ensureEmbeddingFieldIfMissing(mapper.readTree(existing.body()));
            return;
        }
        if (existing.statusCode() != 404) {
            throw new IOException("Unable to inspect Typesense collection, status=" + existing.statusCode() + " body=" + existing.body());
        }
        String payload = """
                {
                  "name": "%s",
                  "fields": [
                    { "name": "id", "type": "string" },
                    { "name": "store_id", "type": "string", "facet": true },
                    { "name": "type", "type": "string", "facet": true },
                    { "name": "scope", "type": "string", "facet": true },
                    { "name": "name", "type": "string", "optional": true },
                    { "name": "mimetype", "type": "string", "facet": true, "optional": true },
                    { "name": "node_type", "type": "string", "facet": true, "optional": true },
                    { "name": "parent", "type": "string", "optional": true },
                    { "name": "content", "type": "string" },
                    {
                      "name": "%s",
                      "type": "float[]",
                      "embed": {
                        "from": ["content", "name"],
                        "model_config": { "model_name": "%s" }
                      }
                    },
                    { "name": "modified_at", "type": "int64", "sort": true }
                  ],
                  "default_sorting_field": "modified_at"
                }
                """.formatted(collection, config.typesense().embedding().field(), config.typesense().embedding().modelName());
        HttpRequest create = baseRequest("/collections")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        sendExpectSuccess(create, "create collection " + collection);
        LOGGER.log(Level.INFO, "Created Typesense collection: {0}", collection);
    }

    private void ensureEmbeddingFieldIfMissing(JsonNode collectionNode) throws IOException, InterruptedException, IndexStoreException {
        String fieldName = config.typesense().embedding().field();
        for (JsonNode field : collectionNode.path("fields")) {
            if (fieldName.equals(field.path("name").asText())) {
                return;
            }
        }
        String payload = """
                {
                  "fields": [
                    {
                      "name": "%s",
                      "type": "float[]",
                      "embed": {
                        "from": ["content", "name"],
                        "model_config": { "model_name": "%s" }
                      }
                    }
                  ]
                }
                """.formatted(fieldName, config.typesense().embedding().modelName());
        HttpRequest patch = baseRequest("/collections/" + encode(config.typesense().collection()))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(payload))
                .build();
        sendExpectSuccess(patch, "add embedding field on collection " + config.typesense().collection());
        LOGGER.log(Level.INFO, "Added embedding field ''{0}'' to Typesense collection {1}", new Object[]{
                fieldName,
                config.typesense().collection()
        });
    }

    private void ensureConversationHistoryCollection() throws IOException, InterruptedException, IndexStoreException {
        String historyCollection = config.typesense().conversation().historyCollection();
        HttpRequest get = baseRequest("/collections/" + encode(historyCollection)).GET().build();
        HttpResponse<String> existing = client.send(get, HttpResponse.BodyHandlers.ofString());
        if (existing.statusCode() == 200) {
            return;
        }
        if (existing.statusCode() != 404) {
            throw new IOException("Unable to inspect Typesense conversation collection, status=" + existing.statusCode() + " body=" + existing.body());
        }
        String payload = """
                {
                  "name": "%s",
                  "fields": [
                    { "name": "conversation_id", "type": "string" },
                    { "name": "model_id", "type": "string" },
                    { "name": "timestamp", "type": "int32" },
                    { "name": "role", "type": "string", "index": false },
                    { "name": "message", "type": "string", "index": false }
                  ]
                }
                """.formatted(historyCollection);
        HttpRequest create = baseRequest("/collections")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        sendExpectSuccess(create, "create conversation history collection " + historyCollection);
        LOGGER.log(Level.INFO, "Created Typesense conversation history collection: {0}", historyCollection);
    }

    private void ensureConversationModel() throws IOException, InterruptedException, IndexStoreException {
        IndexStoreConfig.Conversation conversation = config.typesense().conversation();
        String modelPath = "/conversations/models/" + encode(conversation.modelId());
        HttpRequest get = baseRequest(modelPath).GET().build();
        HttpResponse<String> existing = client.send(get, HttpResponse.BodyHandlers.ofString());
        if (existing.statusCode() == 200) {
            return;
        }
        if (existing.statusCode() != 404) {
            throw new IOException("Unable to inspect Typesense conversation model, status=" + existing.statusCode() + " body=" + existing.body());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", conversation.modelId());
        payload.put("history_collection", conversation.historyCollection());
        payload.put("model_name", conversation.modelName());
        payload.put("vllm_url", conversation.vllmUrl());
        payload.put("system_prompt", conversation.systemPrompt());
        payload.put("ttl", conversation.ttlSeconds());
        payload.put("max_bytes", conversation.maxBytes());
        conversation.apiKey().filter(key -> !key.isBlank()).ifPresent(key -> payload.put("api_key", key));

        HttpRequest create = baseRequest("/conversations/models")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();
        sendExpectSuccess(create, "create conversation model " + conversation.modelId());
        LOGGER.log(Level.INFO, "Created Typesense conversation model: {0}", conversation.modelId());
    }

    private String extractExplain(JsonNode hit, JsonNode document) {
        for (JsonNode highlight : hit.path("highlights")) {
            JsonNode snippet = highlight.path("snippet");
            if (!snippet.isMissingNode() && !snippet.asText().isBlank()) {
                return snippet.asText();
            }
            for (JsonNode snippets : highlight.path("snippets")) {
                if (!snippets.asText().isBlank()) {
                    return snippets.asText();
                }
            }
        }
        JsonNode legacyHighlight = hit.path("highlight");
        if (legacyHighlight.isObject()) {
            for (String field : List.of(IndexStoreDocumentBuilder.CONTENT_FIELD, IndexStoreDocumentBuilder.NAME_FIELD)) {
                JsonNode value = legacyHighlight.path(field);
                if (!value.isMissingNode() && !value.asText().isBlank()) {
                    return value.asText();
                }
            }
        }
        String fallback = document.path(IndexStoreDocumentBuilder.CONTENT_FIELD).asText(document.path(IndexStoreDocumentBuilder.NAME_FIELD).asText(""));
        return fallback.length() > 240 ? fallback.substring(0, 240) : fallback;
    }

    private List<IndexStoreResult> parseHits(JsonNode hitsNode) {
        List<IndexStoreResult> results = new ArrayList<>();
        for (JsonNode hit : hitsNode) {
            JsonNode document = hit.path("document");
            IndexStoreResult result = new IndexStoreResult();
            result.setIdentifier(document.path(IndexStoreDocumentBuilder.ID_FIELD).asText());
            result.setType(document.path(IndexStoreDocumentBuilder.TYPE_FIELD).asText());
            result.setScore((float) hit.path("text_match").asDouble(0));
            result.setExplain(extractExplain(hit, document));
            results.add(result);
        }
        return results;
    }

    private Map<String, Object> buildConversationPayload(String scope) {
        Map<String, Object> search = new LinkedHashMap<>();
        search.put("collection", config.typesense().collection());
        search.put("query_by", config.typesense().embedding().field());
        search.put("filter_by", IndexStoreDocumentBuilder.STORE_ID_FIELD + ":=" + config.typesense().storeId()
                + " && " + IndexStoreDocumentBuilder.SCOPE_FIELD + ":=" + scope);
        search.put("per_page", config.typesense().conversation().perPage());
        search.put("exclude_fields", String.join(",",
                config.typesense().embedding().field(),
                "conversation_history"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("searches", List.of(search));
        return payload;
    }

    private HttpRequest.Builder baseRequest(String path) {
        return baseRequest(path, Duration.ofSeconds(5));
    }

    private HttpRequest.Builder baseRequest(String path, Duration timeout) {
        return HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("X-TYPESENSE-API-KEY", config.typesense().apiKey());
    }

    private HttpResponse<String> sendExpectSuccess(HttpRequest request, String action) throws IOException, InterruptedException, IndexStoreException {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IndexStoreException("Unable to " + action + ", status=" + response.statusCode() + " body=" + response.body());
        }
        return response;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean ensureConversationReady() {
        if (!config.typesense().conversation().enabled()) {
            return false;
        }
        if (conversationReady) {
            return true;
        }

        Instant now = Instant.now();
        if (now.isBefore(lastConversationInitAttempt.plus(CONVERSATION_RETRY_INTERVAL))) {
            return false;
        }

        synchronized (conversationInitLock) {
            if (conversationReady) {
                return true;
            }
            now = Instant.now();
            if (now.isBefore(lastConversationInitAttempt.plus(CONVERSATION_RETRY_INTERVAL))) {
                return false;
            }
            lastConversationInitAttempt = now;
            try {
                ensureConversationHistoryCollection();
                ensureConversationModel();
                conversationReady = true;
                LOGGER.log(Level.INFO, "Typesense conversational search is enabled");
                return true;
            } catch (Exception e) {
                conversationReady = false;
                LOGGER.log(Level.WARNING,
                        "Conversation search initialization failed, continuing with classic search only", e);
                return false;
            }
        }
    }
}
