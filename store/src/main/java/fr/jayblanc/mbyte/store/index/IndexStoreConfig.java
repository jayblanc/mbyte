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

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

/**
 * @author Jerome Blanchard
 */
@ConfigMapping(prefix = "store.index")
public interface IndexStoreConfig {
    Backend backend();

    Bootstrap bootstrap();

    Typesense typesense();

    enum Backend {
        TYPESENSE
    }

    interface Bootstrap {
        boolean reindex();
    }

    interface Typesense {
        String protocol();
        String host();
        int port();
        String apiKey();
        String collection();
        String storeId();
        Embedding embedding();
        Conversation conversation();
    }

    interface Embedding {
        String field();
        String modelName();
    }

    interface Conversation {
        boolean enabled();
        String historyCollection();
        String modelId();
        String modelName();
        String vllmUrl();
        Optional<String> apiKey();
        String systemPrompt();
        int ttlSeconds();
        int maxBytes();
        int perPage();
    }
}
