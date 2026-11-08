package com.pousada.marealta.domain.notification;

public interface NotificationSubscriber {
    String name();
    PersistedNotification handle(DomainEvent event);
}
