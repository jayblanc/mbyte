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
package fr.jayblanc.mbyte.manager.core.services;

import fr.jayblanc.mbyte.manager.core.AccessDeniedException;
import fr.jayblanc.mbyte.manager.core.entity.WebHook;
import fr.jayblanc.mbyte.manager.core.exceptions.WebHookNotFoundException;

import java.util.List;

public interface WebHookService {
    List<WebHook> listWebHooks(String owner) throws AccessDeniedException;
    List<WebHook> listActiveWebHooks(String owner, String event);
    String createWebHook(String owner, String url, List<String> events) throws AccessDeniedException;
    WebHook getWebHook(String id) throws WebHookNotFoundException, AccessDeniedException;
    WebHook updateWebHook(String id, String url, boolean active, List<String> events) throws WebHookNotFoundException, AccessDeniedException;
    void deleteWebHook(String id) throws WebHookNotFoundException, AccessDeniedException;
}
