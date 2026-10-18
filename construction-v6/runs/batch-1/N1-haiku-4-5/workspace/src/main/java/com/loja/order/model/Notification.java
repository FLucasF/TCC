package com.loja.order.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
  public enum Type {
    PAYMENT_APPROVED,
    ORDER_SHIPPED,
    ORDER_CANCELLED
  }

  public enum Status {
    NEW,
    READ
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "customer_cpf", nullable = false, length = 11)
  private String customerCpf;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private Type type;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Notification() {}

  public Notification(String customerCpf, Type type, Long orderId) {
    this.customerCpf = customerCpf;
    this.type = type;
    this.orderId = orderId;
    this.status = Status.NEW;
    this.createdAt = Instant.now();
  }

  public void markAsRead() {
    this.status = Status.READ;
  }

  public Long getId() {
    return id;
  }

  public String getCustomerCpf() {
    return customerCpf;
  }

  public Type getType() {
    return type;
  }

  public Long getOrderId() {
    return orderId;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
