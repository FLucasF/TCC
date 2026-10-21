package com.pousadamarealta.event;

public interface EventHandler<T extends ReservationEvent> {
    void handle(T event);
}
