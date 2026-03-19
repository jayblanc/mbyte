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
package fr.jayblanc.mbyte.store.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.jayblanc.mbyte.store.notification.NotificationService;
import fr.jayblanc.mbyte.store.notification.entity.Event;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ManagerWebhookBridgeListener {

    private static final Logger LOGGER = Logger.getLogger(ManagerWebhookBridgeListener.class.getName());
    private static final String STORE_FILE_CREATED_EVENT = "file.create";

    @Inject ObjectMapper objectMapper;

    @ConfigProperty(name = "store.webhooks.bridge.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "store.webhooks.bridge.manager.url")
    Optional<String> managerUrl;

    @ConfigProperty(name = "store.webhooks.bridge.shared.secret")
    Optional<String> sharedSecret;

    @ConfigProperty(name = "store.webhooks.bridge.timeout-ms", defaultValue = "3000")
    long timeoutMs;

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @ConsumeEvent(value = NotificationService.NOTIFICATION_TOPIC, blocking = true)
    public void onMessage(Event event) {
        if (!enabled || !STORE_FILE_CREATED_EVENT.equals(event.getEventType())) {
            return;
        }
        if (event.getOwner() == null || event.getOwner().isBlank()) {
            LOGGER.log(Level.WARNING, "Skipping store event forwarding because owner is missing for event: {0}", event);
            return;
        }
        if (managerUrl.isEmpty() || managerUrl.get().isBlank()) {
            LOGGER.log(Level.WARNING, "Skipping store event forwarding because manager URL is not configured");
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(managerUrl.get()))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            String bridgeSecret = sharedSecret.orElse("");
            if (!bridgeSecret.isBlank()) {
                requestBuilder.header("X-MByte-Store-Secret", bridgeSecret);
            }

            HttpResponse<Void> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 300) {
                LOGGER.log(Level.WARNING, "Store event forwarding failed with status: {0}", response.statusCode());
            } else {
                LOGGER.log(Level.INFO, "Store event forwarded to manager for owner: {0}", event.getOwner());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to forward store event to manager", e);
        }
    }
}
