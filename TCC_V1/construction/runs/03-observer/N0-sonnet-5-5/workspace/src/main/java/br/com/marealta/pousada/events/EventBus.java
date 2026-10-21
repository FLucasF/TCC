package br.com.marealta.pousada.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventBus {
    private static final Logger LOG = Logger.getLogger(EventBus.class.getName());
    private final List<ReservationListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(ReservationListener listener) {
        listeners.add(listener);
    }

    public void publish(ReservationEvent event) {
        for (ReservationListener l : listeners) {
            try {
                l.on(event);
            } catch (RuntimeException e) {
                // one failing subscriber must not stop the others nor undo the reservation operation
                LOG.log(Level.SEVERE, "Listener " + l.getClass().getSimpleName() + " failed", e);
            }
        }
    }
}
