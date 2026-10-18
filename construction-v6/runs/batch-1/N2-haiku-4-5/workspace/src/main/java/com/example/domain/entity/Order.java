package com.example.domain.entity;

import com.example.domain.enums.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id;

    @Column(nullable = false, length = 11)
    private String customerCpf;

    @Column(nullable = false, length = 100)
    private String customerEmail;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant paidAt;

    @Column
    private String lastSuccessfulTransactionId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    @Convert(converter = OrderSummaryConverter.class)
    private OrderSummary summary;

    @Column
    private String trackingCode;

    protected Order() {
    }

    public Order(String customerCpf, String customerEmail, OrderSummary summary) {
        this.id = UUID.randomUUID().toString();
        this.customerCpf = customerCpf;
        this.customerEmail = customerEmail;
        this.status = OrderStatus.AWAITING_PAYMENT;
        this.createdAt = Instant.now();
        this.summary = summary;
    }

    public String getId() {
        return id;
    }

    public String getCustomerCpf() {
        return customerCpf;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public OrderSummary getSummary() {
        return summary;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void addItem(OrderItem item) {
        this.items.add(item);
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public String getLastSuccessfulTransactionId() {
        return lastSuccessfulTransactionId;
    }

    public void setLastSuccessfulTransactionId(String transactionId) {
        this.lastSuccessfulTransactionId = transactionId;
    }

    public boolean isPaidWithinTimeout(long timeoutMillis) {
        if (this.paidAt == null) {
            return false;
        }
        long elapsed = this.paidAt.toEpochMilli() - this.createdAt.toEpochMilli();
        return elapsed <= timeoutMillis;
    }
}
