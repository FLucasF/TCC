package com.pousadamarealta.domain.events;

public interface EventListener<T extends ReservationEvent> {
    void handle(T event);
}
