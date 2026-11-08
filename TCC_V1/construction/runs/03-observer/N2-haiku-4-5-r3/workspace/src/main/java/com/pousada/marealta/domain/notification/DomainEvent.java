package com.pousada.marealta.domain.notification;

import java.time.LocalDateTime;

public abstract class DomainEvent {
    private final LocalDateTime occurredAt;

    protected DomainEvent() {
        this.occurredAt = LocalDateTime.now();
    }

    public LocalDateTime occurredAt() {
        return occurredAt;
    }
}
