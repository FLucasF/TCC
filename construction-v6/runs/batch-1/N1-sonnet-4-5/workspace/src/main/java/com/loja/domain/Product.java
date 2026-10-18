package com.loja.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id
    private String sku;

    private String name;

    private BigDecimal price;

    private BigDecimal weightKg;

    private int stock;

    private boolean discontinued;

    @Version
    private Long version;

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

    public Money getPrice() {
        return new Money(price);
    }

    public Weight getWeight() {
        return new Weight(weightKg);
    }

    public int getStock() {
        return stock;
    }

    public boolean isDiscontinued() {
        return discontinued;
    }

    public boolean hasStock(int quantity) {
        return stock >= quantity;
    }

    public void decreaseStock(int quantity) {
        if (stock < quantity) {
            throw new IllegalStateException("Insufficient stock for product " + sku);
        }
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }
}
