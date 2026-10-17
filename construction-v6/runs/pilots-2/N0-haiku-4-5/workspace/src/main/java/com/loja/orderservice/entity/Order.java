package com.loja.orderservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "customer_cpf")
    private String customerCpf;

    @Column(name = "customer_email")
    private String customerEmail;

    private String zipCode;

    @Enumerated(EnumType.STRING)
    private Region region;

    @Enumerated(EnumType.STRING)
    private ShippingMethod shippingMethod;

    private String coupon;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private Integer installments;

    @Enumerated(EnumType.STRING)
    private ClubTier clubTier;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Embedded
    private OrderSummary summary;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    private String trackingCode;

    private String lastTransactionId;

    private Instant createdAt;

    private Instant paidAt;

    public Order() {}

    public Order(String customerCpf, String customerEmail, String zipCode, Region region,
                 ShippingMethod shippingMethod, String coupon, PaymentMethod paymentMethod,
                 Integer installments, ClubTier clubTier) {
        this.customerCpf = customerCpf;
        this.customerEmail = customerEmail;
        this.zipCode = zipCode;
        this.region = region;
        this.shippingMethod = shippingMethod;
        this.coupon = coupon;
        this.paymentMethod = paymentMethod;
        this.installments = installments;
        this.clubTier = clubTier;
        this.status = OrderStatus.AWAITING_PAYMENT;
        this.createdAt = Instant.now();
        this.trackingCode = null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerCpf() { return customerCpf; }
    public void setCustomerCpf(String customerCpf) { this.customerCpf = customerCpf; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public ShippingMethod getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(ShippingMethod shippingMethod) { this.shippingMethod = shippingMethod; }

    public String getCoupon() { return coupon; }
    public void setCoupon(String coupon) { this.coupon = coupon; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }

    public ClubTier getClubTier() { return clubTier; }
    public void setClubTier(ClubTier clubTier) { this.clubTier = clubTier; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public OrderSummary getSummary() { return summary; }
    public void setSummary(OrderSummary summary) { this.summary = summary; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

    public String getLastTransactionId() { return lastTransactionId; }
    public void setLastTransactionId(String lastTransactionId) { this.lastTransactionId = lastTransactionId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
