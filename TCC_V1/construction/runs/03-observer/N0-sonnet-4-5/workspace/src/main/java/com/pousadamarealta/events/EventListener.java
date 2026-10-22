package com.pousadamarealta.events;

public interface EventListener<T extends Event> {
    void handle(T event);
}
