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

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    private Integer stock;

    private Boolean discontinued;

    public Product() {}

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Boolean getDiscontinued() { return discontinued; }
    public void setDiscontinued(Boolean discontinued) { this.discontinued = discontinued; }
}
