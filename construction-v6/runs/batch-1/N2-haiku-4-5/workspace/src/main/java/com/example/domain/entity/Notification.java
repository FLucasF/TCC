package com.example.domain.entity;

import com.example.domain.enums.NotificationType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private String id;

    @Column(nullable = false, length = 11)
    private String customerCpf;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(String customerCpf, NotificationType type, String orderId) {
        this.id = UUID.randomUUID().toString();
        this.customerCpf = customerCpf;
        this.type = type;
        this.orderId = orderId;
        this.status = NotificationStatus.NEW;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getCustomerCpf() {
        return customerCpf;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markAsRead() {
        this.status = NotificationStatus.READ;
    }

    public enum NotificationStatus {
        NEW,
        READ
    }
}
