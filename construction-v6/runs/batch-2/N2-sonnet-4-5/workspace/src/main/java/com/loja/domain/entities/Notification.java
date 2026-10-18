package com.loja.domain.entities;

public class Notification {
    private final long id;
    private final NotificationType type;
    private final String orderId;
    private NotificationStatus status;

    public Notification(long id, NotificationType type, String orderId, NotificationStatus status) {
        this.id = id;
        this.type = type;
        this.orderId = orderId;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public NotificationType getType() {
        return type;
    }

    public String getOrderId() {
        return orderId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void markAsRead() {
        this.status = NotificationStatus.READ;
    }
}
