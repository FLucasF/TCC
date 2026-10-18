package com.loja.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerCpf;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private String trackingCode;

    private BigDecimal productsSubtotal;
    private BigDecimal couponDiscount;
    private BigDecimal shippingCost;
    private int deliveryDays;
    private BigDecimal insurance;
    private BigDecimal paymentAdjustment;
    private BigDecimal finalTotal;
    private int installments;
    private BigDecimal installmentAmount;
    private BigDecimal nextPurchaseCredit;
    private boolean gift;

    private String paymentTransactionId;

    @Version
    private Long version;

    protected Order() {}

    public Order(String customerCpf, List<OrderItem> items, OrderSummary summary) {
        this.customerCpf = customerCpf;
        this.status = OrderStatus.AWAITING_PAYMENT;
        this.createdAt = Instant.now();
        this.items = new ArrayList<>(items);
        for (OrderItem item : this.items) {
            item.setOrder(this);
        }

        this.productsSubtotal = summary.productsSubtotal().value();
        this.couponDiscount = summary.couponDiscount().value();
        this.shippingCost = summary.shippingCost().value();
        this.deliveryDays = summary.deliveryDays();
        this.insurance = summary.insurance().value();
        this.paymentAdjustment = summary.paymentAdjustment().value();
        this.finalTotal = summary.finalTotal().value();
        this.installments = summary.installments();
        this.installmentAmount = summary.installmentAmount().value();
        this.nextPurchaseCredit = summary.nextPurchaseCredit().value();
        this.gift = summary.gift();
    }

    public Long getId() {
        return id;
    }

    public String getCustomerCpf() {
        return customerCpf;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return new ArrayList<>(items);
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public OrderSummary getSummary() {
        return new OrderSummary(
            new Money(productsSubtotal),
            new Money(couponDiscount),
            new Money(shippingCost),
            deliveryDays,
            new Money(insurance),
            new Money(paymentAdjustment),
            new Money(finalTotal),
            installments,
            new Money(installmentAmount),
            new Money(nextPurchaseCredit),
            gift
        );
    }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public boolean isExpired() {
        if (status != OrderStatus.AWAITING_PAYMENT) {
            return false;
        }
        Instant expirationTime = createdAt.plusSeconds(48 * 3600);
        return Instant.now().isAfter(expirationTime);
    }

    public void markAsPaid(String transactionId) {
        if (status != OrderStatus.AWAITING_PAYMENT) {
            throw new IllegalStateException("Order cannot be paid in status " + status);
        }
        this.status = OrderStatus.PAID;
        this.paymentTransactionId = transactionId;
    }

    public void ship(String trackingCode) {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("Order cannot be shipped in status " + status);
        }
        this.status = OrderStatus.SHIPPED;
        this.trackingCode = trackingCode;
    }

    public void deliver() {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Order cannot be delivered in status " + status);
        }
        this.status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order cannot be cancelled in status " + status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isPaid() {
        return status == OrderStatus.PAID || status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED;
    }
}
