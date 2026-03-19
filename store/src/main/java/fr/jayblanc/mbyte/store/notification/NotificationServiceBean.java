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
package fr.jayblanc.mbyte.store.notification;

import fr.jayblanc.mbyte.store.notification.entity.Event;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionScoped;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

@TransactionScoped
public class NotificationServiceBean implements NotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());

    private final Queue<Event> events = new ConcurrentLinkedQueue<>();

    @Inject EventBus bus;
    @Inject TransactionManager tm;

    public NotificationServiceBean() {
        LOGGER.log(Level.INFO, "Creating NotificationServiceBean");
    }

    @Override public void notify(String type, String source) throws NotificationServiceException {
        notify(null, type, source);
    }

    @Override public void notify(String owner, String type, String source) throws NotificationServiceException {
        LOGGER.log(Level.INFO, "Throwing event of type: " + type);
        try {
            events.add(Event.build(owner, type, source));
        } catch (Exception e) {
            throw new NotificationServiceException("Unable to throw event", e);
        }
    }

    private void publishEvent(Event event) {
        LOGGER.log(Level.INFO, "Publishing event: " + event.toString());
        bus.publish(NOTIFICATION_TOPIC, event);
    }

    @PreDestroy
    void onBeforeEndTransaction() {
        try {
            LOGGER.log(Level.INFO, "Transaction ended: " + tm.getTransaction() + " with status: " + tm.getStatus());
            if (tm.getStatus() == Status.STATUS_COMMITTED) {
                events.forEach(this::publishEvent);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to publish events after commit", e);
        }
    }

}
