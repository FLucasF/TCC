package com.pousada.marealta.domain.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public class PersistedNotification {
    private final String id;
    private final long reservationId;
    private final String channel;
    private final String recipient;
    private final String subject;
    private final String body;
    private final boolean success;
    private final String errorMessage;
    private final LocalDateTime sentAt;

    public PersistedNotification(
            String id,
            long reservationId,
            String channel,
            String recipient,
            String subject,
            String body,
            boolean success,
            String errorMessage,
            LocalDateTime sentAt
    ) {
        this.id = id;
        this.reservationId = reservationId;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.success = success;
        this.errorMessage = errorMessage;
        this.sentAt = sentAt;
    }

    public static PersistedNotification success(
            long reservationId,
            String channel,
            String recipient,
            String subject,
            String body
    ) {
        return new PersistedNotification(
                UUID.randomUUID().toString(),
                reservationId,
                channel,
                recipient,
                subject,
                body,
                true,
                null,
                LocalDateTime.now()
        );
    }

    public static PersistedNotification failure(
            long reservationId,
            String channel,
            String recipient,
            String subject,
            String body,
            String errorMessage
    ) {
        return new PersistedNotification(
                UUID.randomUUID().toString(),
                reservationId,
                channel,
                recipient,
                subject,
                body,
                false,
                errorMessage,
                LocalDateTime.now()
        );
    }

    public static PersistedNotification noop() {
        return null;
    }

    public String id() {
        return id;
    }

    public long reservationId() {
        return reservationId;
    }

    public String channel() {
        return channel;
    }

    public String recipient() {
        return recipient;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }

    public boolean success() {
        return success;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public LocalDateTime sentAt() {
        return sentAt;
    }
}
