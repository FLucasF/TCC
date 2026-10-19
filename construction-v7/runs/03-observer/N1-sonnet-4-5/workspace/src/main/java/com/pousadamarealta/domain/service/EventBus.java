package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.event.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventBus {
    private final List<Consumer<Event>> listeners = new ArrayList<>();

    public void subscribe(Consumer<Event> listener) {
        listeners.add(listener);
    }

    public void publish(Event event) {
        listeners.forEach(listener -> listener.accept(event));
    }
}
