package com.loja.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerCpf;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    protected Notification() {}

    public Notification(String customerCpf, NotificationType type, Long orderId) {
        this.customerCpf = customerCpf;
        this.type = type;
        this.orderId = orderId;
        this.status = NotificationStatus.NEW;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerCpf() {
        return customerCpf;
    }

    public NotificationType getType() {
        return type;
    }

    public Long getOrderId() {
        return orderId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void markAsRead() {
        this.status = NotificationStatus.READ;
    }
}
