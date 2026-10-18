package com.loja.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
  @Id
  @Column(name = "sku", length = 20)
  private String sku;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "price", nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "weight_kg", nullable = false)
  private BigDecimal weightKg;

  @Column(name = "stock", nullable = false)
  private int stock;

  @Column(name = "discontinued", nullable = false)
  private boolean discontinued;

  protected Product() {}

  public Product(String sku, String name, BigDecimal price, BigDecimal weightKg, int stock, boolean discontinued) {
    this.sku = sku;
    this.name = name;
    this.price = price;
    this.weightKg = weightKg;
    this.stock = stock;
    this.discontinued = discontinued;
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public BigDecimal getWeightKg() {
    return weightKg;
  }

  public int getStock() {
    return stock;
  }

  public boolean isDiscontinued() {
    return discontinued;
  }

  public void decreaseStock(int quantity) {
    this.stock -= quantity;
  }

  public void increaseStock(int quantity) {
    this.stock += quantity;
  }

  public boolean hasEnoughStock(int quantity) {
    return stock >= quantity;
  }
}
