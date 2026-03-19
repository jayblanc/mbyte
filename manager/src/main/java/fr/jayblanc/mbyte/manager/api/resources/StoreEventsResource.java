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
package fr.jayblanc.mbyte.manager.api.resources;

import fr.jayblanc.mbyte.manager.api.dto.StoreEventDto;
import fr.jayblanc.mbyte.manager.core.WebHookDispatchService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("internal/store-events")
public class StoreEventsResource {

    private static final Logger LOGGER = Logger.getLogger(StoreEventsResource.class.getName());
    private static final String SECRET_HEADER = "X-MByte-Store-Secret";

    @Inject WebHookDispatchService dispatchService;

    @ConfigProperty(name = "manager.webhooks.bridge.shared-secret")
    Optional<String> sharedSecret;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response onStoreEvent(@HeaderParam(SECRET_HEADER) String providedSecret, StoreEventDto event) {
        String bridgeSecret = sharedSecret.orElse("");
        if (!bridgeSecret.isBlank() && !bridgeSecret.equals(providedSecret)) {
            LOGGER.log(Level.WARNING, "Denied store event due to invalid bridge secret");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        LOGGER.log(Level.INFO, "Received store event for owner={0}, type={1}", new Object[]{event != null ? event.getOwner() : null, event != null ? event.getEventType() : null});
        dispatchService.dispatchStoreEvent(event);
        return Response.accepted().build();
    }
}
