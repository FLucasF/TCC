package com.pousadamarealta.service;

import com.pousadamarealta.domain.events.ReservationEvent;
import com.pousadamarealta.domain.events.EventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventPublisher {
    private final Map<Class<? extends ReservationEvent>, List<EventListener<? extends ReservationEvent>>> listeners = new HashMap<>();

    public <T extends ReservationEvent> void register(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends ReservationEvent> void publish(T event) {
        List<EventListener<? extends ReservationEvent>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener<? extends ReservationEvent> listener : eventListeners) {
                ((EventListener<T>) listener).handle(event);
            }
        }
    }
}
