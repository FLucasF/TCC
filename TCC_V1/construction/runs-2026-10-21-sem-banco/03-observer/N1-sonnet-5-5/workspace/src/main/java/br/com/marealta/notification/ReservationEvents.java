package br.com.marealta.notification;

import java.util.ArrayList;
import java.util.List;

/** Entrega cada evento a todos os interessados; a falha de um não impede o aviso dos outros. */
public class ReservationEvents {
    private static final System.Logger LOG = System.getLogger(ReservationEvents.class.getName());

    private final List<ReservationListener> listeners = new ArrayList<>();

    public ReservationEvents(List<? extends ReservationListener> listeners) {
        this.listeners.addAll(listeners);
    }

    public void publish(ReservationEvent event) {
        for (ReservationListener listener : listeners) {
            try {
                listener.on(event);
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.ERROR,
                        "Falha ao avisar " + listener.getClass().getSimpleName(), e);
            }
        }
    }
}
