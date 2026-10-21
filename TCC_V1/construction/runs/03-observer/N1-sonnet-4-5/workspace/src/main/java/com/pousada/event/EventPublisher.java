package com.pousada.event;

import java.util.ArrayList;
import java.util.List;

public class EventPublisher {
    private final List<EventListener> listeners = new ArrayList<>();

    public void register(EventListener listener) {
        listeners.add(listener);
    }

    public void publish(ReservationEvent event) {
        for (EventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
