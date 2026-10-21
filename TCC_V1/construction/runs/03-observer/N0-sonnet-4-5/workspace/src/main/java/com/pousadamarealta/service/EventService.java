package com.pousadamarealta.service;

import com.pousadamarealta.event.EventHandler;
import com.pousadamarealta.event.ReservationEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventService {
    private final Map<Class<? extends ReservationEvent>, List<EventHandler<? extends ReservationEvent>>> handlers = new HashMap<>();

    public <T extends ReservationEvent> void register(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @SuppressWarnings("unchecked")
    public <T extends ReservationEvent> void publish(T event) {
        List<EventHandler<? extends ReservationEvent>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (EventHandler<? extends ReservationEvent> handler : eventHandlers) {
                ((EventHandler<T>) handler).handle(event);
            }
        }
    }
}
