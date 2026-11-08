package com.pousadamarealta.domain.event;

public interface EventListener<T extends DomainEvent> {
    void handle(T event);
}
