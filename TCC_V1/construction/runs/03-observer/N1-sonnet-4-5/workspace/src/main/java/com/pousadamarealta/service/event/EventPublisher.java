package com.pousadamarealta.service.event;

import com.pousadamarealta.domain.events.ReservationEvent;

import java.util.ArrayList;
import java.util.List;

public class EventPublisher {
    private final List<EventListener> listeners = new ArrayList<>();

    public void subscribe(EventListener listener) {
        listeners.add(listener);
    }

    public void publish(ReservationEvent event) {
        for (EventListener listener : listeners) {
            listener.handle(event);
        }
    }
}
