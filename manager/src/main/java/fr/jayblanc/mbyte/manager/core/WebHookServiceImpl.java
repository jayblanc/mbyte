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

import fr.jayblanc.mbyte.manager.core.AccessDeniedException;
import fr.jayblanc.mbyte.manager.core.entity.WebHook;
import fr.jayblanc.mbyte.manager.core.exceptions.WebHookNotFoundException;
import fr.jayblanc.mbyte.manager.core.services.WebHookService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

@ApplicationScoped
public class WebHookServiceImpl implements WebHookService {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<WebHook> listWebHooks(String owner) {
        return em.createQuery("SELECT w FROM WebHook w WHERE w.userId = :owner", WebHook.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    @Override
    public List<WebHook> listActiveWebHooks(String owner, String event) {
        return em.createQuery("SELECT DISTINCT w FROM WebHook w JOIN w.events e WHERE w.userId = :owner AND w.active = true AND e = :event", WebHook.class)
                .setParameter("owner", owner)
                .setParameter("event", event)
                .getResultList();
    }

    @Override
    @Transactional
    public String createWebHook(String owner, String url, List<String> events) {
        WebHook wh = new WebHook();
        wh.setUserId(owner);
        wh.setUrl(url);
        wh.setEvents(new HashSet<>(events));

        // Generate a random signing secret
        byte[] randomBytes = new byte[24];
        new SecureRandom().nextBytes(randomBytes);
        wh.setSecret(Base64.getEncoder().encodeToString(randomBytes));

        em.persist(wh);
        return wh.getId();
    }

    @Override
    public WebHook getWebHook(String id) throws WebHookNotFoundException {
        WebHook wh = em.find(WebHook.class, id);
        if (wh == null) throw new WebHookNotFoundException(id);
        return wh;
    }

    @Override
    @Transactional
    public WebHook updateWebHook(String id, String url, boolean active, List<String> events) throws WebHookNotFoundException {
        WebHook wh = getWebHook(id);
        wh.setUrl(url);
        wh.setActive(active);
        wh.setEvents(new HashSet<>(events));
        return em.merge(wh);
    }

    @Override
    @Transactional
    public void deleteWebHook(String id) throws WebHookNotFoundException {
        WebHook wh = getWebHook(id);
        em.remove(wh);
    }
}
