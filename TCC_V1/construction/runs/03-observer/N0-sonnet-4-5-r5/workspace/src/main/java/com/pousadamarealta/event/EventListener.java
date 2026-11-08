package com.pousadamarealta.event;

public interface EventListener<T extends ReservationEvent> {
    void handle(T event);
}
