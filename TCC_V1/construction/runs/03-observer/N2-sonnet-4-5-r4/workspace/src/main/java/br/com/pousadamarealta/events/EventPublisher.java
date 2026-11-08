package br.com.pousadamarealta.events;

import java.util.ArrayList;
import java.util.List;

public class EventPublisher {
    private final List<ReservationEventListener> listeners = new ArrayList<>();

    public void subscribe(ReservationEventListener listener) {
        listeners.add(listener);
    }

    public void publish(ReservationEvent event) {
        for (ReservationEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
