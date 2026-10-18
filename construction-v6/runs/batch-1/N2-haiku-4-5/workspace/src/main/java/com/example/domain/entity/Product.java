package com.example.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 10)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Boolean discontinued;

    protected Product() {
    }

    public Product(String sku, String name, BigDecimal price, BigDecimal weightKg, Integer stock, Boolean discontinued) {
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

    public Integer getStock() {
        return stock;
    }

    public Boolean isDiscontinued() {
        return discontinued;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}
