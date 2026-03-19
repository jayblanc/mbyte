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

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@ConfigMapping(prefix = "manager")
public interface CoreConfig {

    String instance();

    @WithName("store")
    StoreConfig store();

    @WithName("webhooks")
    Optional<WebHooksConfig> webhooks();

    interface StoreConfig {

        String image();

        String version();

        String domain();

    }

    interface WebHooksConfig {

        @WithName("bridge")
        Optional<BridgeConfig> bridge();

        interface BridgeConfig {

            @WithName("shared-secret")
            Optional<String> sharedSecret();

            @WithName("store-events-url")
            Optional<String> storeEventsUrl();

        }

    }

}
