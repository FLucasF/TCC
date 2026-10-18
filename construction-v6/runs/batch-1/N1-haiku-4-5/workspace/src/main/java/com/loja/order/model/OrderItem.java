package com.loja.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "sku", nullable = false, length = 20)
  private String sku;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "price", nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  protected OrderItem() {}

  public OrderItem(String sku, int quantity, BigDecimal price) {
    this.sku = sku;
    this.quantity = quantity;
    this.price = price;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public Long getId() {
    return id;
  }

  public Order getOrder() {
    return order;
  }

  public String getSku() {
    return sku;
  }

  public int getQuantity() {
    return quantity;
  }

  public BigDecimal getPrice() {
    return price;
  }
}
