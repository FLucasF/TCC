package com.loja.model;

public class Notification {
    public String id;
    public NotificationType type;
    public String orderId;
    public String cpf;
    public NotificationStatus status;

    public Notification() {}

    public Notification(String id, NotificationType type, String orderId, String cpf, NotificationStatus status) {
        this.id = id;
        this.type = type;
        this.orderId = orderId;
        this.cpf = cpf;
        this.status = status;
    }
}
