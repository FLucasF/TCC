package com.pousadamarealta.domain.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventPublisher {
    private final Map<Class<? extends DomainEvent>, List<EventListener<? extends DomainEvent>>> listeners = new HashMap<>();

    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public <T extends DomainEvent> void publish(T event) {
        List<EventListener<? extends DomainEvent>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                listener.handle(event);
            }
        }
    }
}
