package com.pousadamarealta.domain.event;

public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
}
