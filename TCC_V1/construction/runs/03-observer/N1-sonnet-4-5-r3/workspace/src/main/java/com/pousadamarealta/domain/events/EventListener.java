package com.pousadamarealta.domain.events;

public interface EventListener<T extends DomainEvent> {
    void handle(T event);
}
