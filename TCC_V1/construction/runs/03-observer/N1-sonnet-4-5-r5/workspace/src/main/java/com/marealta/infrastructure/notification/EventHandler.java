package com.marealta.infrastructure.notification;

import com.marealta.domain.event.DomainEvent;

public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
}
