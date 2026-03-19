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
package fr.jayblanc.mbyte.manager.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.jayblanc.mbyte.manager.api.dto.StoreEventDto;
import fr.jayblanc.mbyte.manager.core.entity.WebHook;
import fr.jayblanc.mbyte.manager.core.services.WebHookService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class WebHookDispatchService {

    private static final Logger LOGGER = Logger.getLogger(WebHookDispatchService.class.getName());

    private static final String STORE_EVENT_FILE_CREATE = "file.create";
    private static final String WEBHOOK_EVENT_FILE_UPLOAD = "FILE_UPLOAD";
    private static final String SIGNATURE_HEADER = "X-MByte-Signature";
    private static final String SIGNATURE_PREFIX = "sha256=";

    @Inject WebHookService webHookService;
    @Inject ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public void dispatchStoreEvent(StoreEventDto event) {
        if (event == null) {
            LOGGER.log(Level.WARNING, "Ignoring null store event");
            return;
        }
        if (event.getOwner() == null || event.getOwner().isBlank()) {
            LOGGER.log(Level.WARNING, "Ignoring store event without owner: {0}", event);
            return;
        }

        String webhookEventType = toWebHookEventType(event.getEventType());
        if (webhookEventType == null) {
            LOGGER.log(Level.FINE, "Ignoring unsupported store event type: {0}", event.getEventType());
            return;
        }

        List<WebHook> webhooks = webHookService.listActiveWebHooks(event.getOwner(), webhookEventType);
        if (webhooks.isEmpty()) {
            LOGGER.log(Level.INFO, "No active webhook found for owner={0}, event={1}", new Object[]{event.getOwner(), webhookEventType});
            return;
        }

        WebHookEventPayload payload = WebHookEventPayload.buildFrom(event, webhookEventType);
        for (WebHook webHook : webhooks) {
            sendWebHook(webHook, payload);
        }
    }

    private void sendWebHook(WebHook webHook, WebHookEventPayload payload) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(webHook.getUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            if (webHook.getSecret() != null && !webHook.getSecret().isBlank()) {
                requestBuilder.header(SIGNATURE_HEADER, SIGNATURE_PREFIX + sign(body, webHook.getSecret()));
            }

            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                LOGGER.log(Level.WARNING, "Webhook call failed for id={0}, status={1}, body={2}",
                        new Object[]{webHook.getId(), response.statusCode(), response.body()});
            } else {
                LOGGER.log(Level.INFO, "Webhook call succeeded for id={0}, status={1}",
                        new Object[]{webHook.getId(), response.statusCode()});
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Webhook call failed for id=" + webHook.getId(), e);
        }
    }

    private String toWebHookEventType(String storeEventType) {
        if (STORE_EVENT_FILE_CREATE.equals(storeEventType)) {
            return WEBHOOK_EVENT_FILE_UPLOAD;
        }
        return null;
    }

    private String sign(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private record WebHookEventPayload(String content) {

        static WebHookEventPayload buildFrom(StoreEventDto event, String type) {
//            String id = (event.getId() == null || event.getId().isBlank()) ? UUID.randomUUID().toString() : event.getId();
//            long timestamp = event.getTimestamp() <= 0 ? System.currentTimeMillis() : event.getTimestamp();
            return new WebHookEventPayload(new StringBuilder("L'utilisateur ")
                    .append(event.getOwner())
                    .append(" a eu l'événement ")
                    .append(type)
                    .append(" dans son cloud")
                    .toString());
        }
    }
}
