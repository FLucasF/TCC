package br.com.marealta.events;

import java.util.List;

/** Distribui cada evento a todos os interessados registrados. */
public final class ReservationEvents {
    private final List<ReservationListener> listeners;

    public ReservationEvents(List<ReservationListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    public void publish(ReservationEvent event) {
        for (ReservationListener listener : listeners) {
            switch (event) {
                case ReservationEvent.Confirmed e -> listener.onConfirmed(e);
                case ReservationEvent.Cancelled e -> listener.onCancelled(e);
            }
        }
    }
}
