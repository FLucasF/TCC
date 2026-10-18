package com.shop.model;

import com.shop.model.enums.NotificationStatus;
import com.shop.model.enums.NotificationType;

public class Notification {
    private Long id;
    private NotificationType type;
    private Long orderId;
    private String customerCpf;
    private NotificationStatus status;

    public Notification() {}

    public Notification(NotificationType type, Long orderId, String customerCpf) {
        this.type = type;
        this.orderId = orderId;
        this.customerCpf = customerCpf;
        this.status = NotificationStatus.NEW;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getCustomerCpf() { return customerCpf; }
    public void setCustomerCpf(String customerCpf) { this.customerCpf = customerCpf; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
}
