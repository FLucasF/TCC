package com.pousadamarealta.domain.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {
    private final Map<Class<? extends DomainEvent>, List<EventHandler<? extends DomainEvent>>> handlers = new HashMap<>();

    public <T extends DomainEvent> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void publish(T event) {
        List<EventHandler<? extends DomainEvent>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler<? extends DomainEvent> handler : eventHandlers) {
                ((EventHandler<T>) handler).handle(event);
            }
        }
    }
}
