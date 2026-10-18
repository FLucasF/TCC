package com.loja.domain;

public class Notification {
    private String id;
    private String customerCpf;
    private NotificationType type;
    private String orderId;
    private NotificationStatus status;

    public Notification() {
    }

    public Notification(String id, String customerCpf, NotificationType type, String orderId, NotificationStatus status) {
        this.id = id;
        this.customerCpf = customerCpf;
        this.type = type;
        this.orderId = orderId;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerCpf() {
        return customerCpf;
    }

    public void setCustomerCpf(String customerCpf) {
        this.customerCpf = customerCpf;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }
}
