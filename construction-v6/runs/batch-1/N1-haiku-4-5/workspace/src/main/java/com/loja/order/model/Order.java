package com.loja.order.model;

import com.loja.order.domain.*;
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
  @Column(name = "id")
  private Long id;

  @Column(name = "customer_cpf", nullable = false, length = 11)
  private String customerCpf;

  @Column(name = "customer_email", nullable = false)
  private String customerEmail;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OrderStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "tracking_code")
  private String trackingCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "region", nullable = false)
  private Region region;

  @Enumerated(EnumType.STRING)
  @Column(name = "shipping_method", nullable = false)
  private ShippingMethod shippingMethod;

  @Column(name = "zip_code", nullable = false)
  private String zipCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false)
  private PaymentMethod paymentMethod;

  @Column(name = "installments", nullable = false)
  private int installments;

  @Enumerated(EnumType.STRING)
  @Column(name = "club_tier", nullable = false)
  private ClubTier clubTier;

  @Column(name = "coupon")
  private String coupon;

  @Column(name = "transaction_id")
  private String transactionId;

  @Column(name = "paid_at")
  private Instant paidAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  @Embedded
  private OrderSummary summary;

  protected Order() {}

  public Order(
    String customerCpf,
    String customerEmail,
    Region region,
    ShippingMethod shippingMethod,
    String zipCode,
    PaymentMethod paymentMethod,
    int installments,
    ClubTier clubTier,
    String coupon
  ) {
    this.customerCpf = customerCpf;
    this.customerEmail = customerEmail;
    this.status = OrderStatus.AWAITING_PAYMENT;
    this.createdAt = Instant.now();
    this.region = region;
    this.shippingMethod = shippingMethod;
    this.zipCode = zipCode;
    this.paymentMethod = paymentMethod;
    this.installments = installments;
    this.clubTier = clubTier;
    this.coupon = coupon;
  }

  public void setSummary(OrderSummary summary) {
    this.summary = summary;
  }

  public void addItem(String sku, int quantity, BigDecimal price) {
    OrderItem item = new OrderItem(sku, quantity, price);
    item.setOrder(this);
    items.add(item);
  }

  public void markAsPaid(String transactionId, Instant paidAt) {
    this.transactionId = transactionId;
    this.paidAt = paidAt;
    this.status = OrderStatus.PAID;
  }

  public void markAsShipped(String trackingCode) {
    this.trackingCode = trackingCode;
    this.status = OrderStatus.SHIPPED;
  }

  public void markAsDelivered() {
    this.status = OrderStatus.DELIVERED;
  }

  public void cancel() {
    this.status = OrderStatus.CANCELLED;
  }

  public Long getId() {
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

  public String getTrackingCode() {
    return trackingCode;
  }

  public Region getRegion() {
    return region;
  }

  public ShippingMethod getShippingMethod() {
    return shippingMethod;
  }

  public String getZipCode() {
    return zipCode;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public int getInstallments() {
    return installments;
  }

  public ClubTier getClubTier() {
    return clubTier;
  }

  public String getCoupon() {
    return coupon;
  }

  public String getTransactionId() {
    return transactionId;
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

  public boolean isExpired() {
    return Instant.now().isAfter(createdAt.plusSeconds(48 * 3600));
  }

  public boolean isExpiredForPayment() {
    return createdAt.plusSeconds(48 * 3600).isBefore(Instant.now());
  }

  public boolean canBeCancelled() {
    return status != OrderStatus.SHIPPED && status != OrderStatus.DELIVERED && status != OrderStatus.CANCELLED;
  }

  public boolean canBeShipped() {
    return status == OrderStatus.PAID;
  }

  public boolean canBeDelivered() {
    return status == OrderStatus.SHIPPED;
  }
}
