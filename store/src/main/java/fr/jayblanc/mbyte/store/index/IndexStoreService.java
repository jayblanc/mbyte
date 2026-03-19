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

import java.io.InputStream;
import java.util.List;

public interface IndexStoreService {

    void clearStoreDocuments() throws IndexStoreException;

    void index(IndexableContent object) throws IndexStoreException;

    void remove(String identifier) throws IndexStoreException;

    List<IndexStoreResult> search(String scope, String query) throws IndexStoreException;

    IndexStoreConversationResult converse(String scope, String query, String conversationId) throws IndexStoreException;

    InputStream converseStream(String scope, String query, String conversationId) throws IndexStoreException;

}
