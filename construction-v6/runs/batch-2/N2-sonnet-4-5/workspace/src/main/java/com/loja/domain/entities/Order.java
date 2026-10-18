package com.loja.domain.entities;

import java.time.Instant;
import java.util.List;

public class Order {
    private final String id;
    private final String customerCpf;
    private final String customerEmail;
    private final List<OrderItem> items;
    private final OrderSummary summary;
    private OrderStatus status;
    private final Instant createdAt;
    private String trackingCode;
    private String paymentTransactionId;
    private Instant paidAt;

    public Order(
            String id,
            String customerCpf,
            String customerEmail,
            List<OrderItem> items,
            OrderSummary summary,
            OrderStatus status,
            Instant createdAt) {
        this.id = id;
        this.customerCpf = customerCpf;
        this.customerEmail = customerEmail;
        this.items = List.copyOf(items);
        this.summary = summary;
        this.status = status;
        this.createdAt = createdAt;
        this.trackingCode = null;
        this.paymentTransactionId = null;
        this.paidAt = null;
    }

    public Order(
            String id,
            String customerCpf,
            String customerEmail,
            List<OrderItem> items,
            OrderSummary summary,
            OrderStatus status,
            Instant createdAt,
            String trackingCode,
            String paymentTransactionId,
            Instant paidAt) {
        this.id = id;
        this.customerCpf = customerCpf;
        this.customerEmail = customerEmail;
        this.items = List.copyOf(items);
        this.summary = summary;
        this.status = status;
        this.createdAt = createdAt;
        this.trackingCode = trackingCode;
        this.paymentTransactionId = paymentTransactionId;
        this.paidAt = paidAt;
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

    public List<OrderItem> getItems() {
        return items;
    }

    public OrderSummary getSummary() {
        return summary;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public boolean isExpired(Instant now) {
        long hoursSinceCreation = (now.toEpochMilli() - createdAt.toEpochMilli()) / 1000 / 3600;
        return hoursSinceCreation > 48;
    }

    public void confirmPayment(String transactionId, Instant paidAt) {
        this.paymentTransactionId = transactionId;
        this.paidAt = paidAt;
        this.status = OrderStatus.PAID;
    }

    public void ship(String trackingCode) {
        this.trackingCode = trackingCode;
        this.status = OrderStatus.SHIPPED;
    }

    public void deliver() {
        this.status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public boolean canBePaid() {
        return status == OrderStatus.AWAITING_PAYMENT;
    }

    public boolean canBeShipped() {
        return status == OrderStatus.PAID;
    }

    public boolean canBeDelivered() {
        return status == OrderStatus.SHIPPED;
    }

    public boolean canBeCancelled() {
        return status == OrderStatus.AWAITING_PAYMENT || status == OrderStatus.PAID;
    }
}
