package com.loja.models;

public class Notification {
    public String id;
    public Enums.NotificationType type;
    public String orderId;
    public Enums.NotificationStatus status;

    public Notification(String id, Enums.NotificationType type, String orderId, Enums.NotificationStatus status) {
        this.id = id;
        this.type = type;
        this.orderId = orderId;
        this.status = status;
    }
}
