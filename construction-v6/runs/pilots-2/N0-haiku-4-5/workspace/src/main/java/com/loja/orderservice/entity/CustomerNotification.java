package com.loja.orderservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer_notifications")
public class CustomerNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "customer_cpf")
    private String customerCpf;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String orderId;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private Instant createdAt;

    public CustomerNotification() {}

    public CustomerNotification(String customerCpf, NotificationType type, String orderId) {
        this.customerCpf = customerCpf;
        this.type = type;
        this.orderId = orderId;
        this.status = NotificationStatus.NEW;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerCpf() { return customerCpf; }
    public void setCustomerCpf(String customerCpf) { this.customerCpf = customerCpf; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
