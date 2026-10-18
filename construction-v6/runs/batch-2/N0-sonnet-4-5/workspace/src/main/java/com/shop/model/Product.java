package com.shop.model;

import java.math.BigDecimal;

public class Product {
    private String sku;
    private String name;
    private BigDecimal price;
    private BigDecimal weightKg;
    private int stock;
    private boolean discontinued;

    public Product() {}

    public Product(String sku, String name, BigDecimal price, BigDecimal weightKg, int stock, boolean discontinued) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.weightKg = weightKg;
        this.stock = stock;
        this.discontinued = discontinued;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public boolean isDiscontinued() { return discontinued; }
    public void setDiscontinued(boolean discontinued) { this.discontinued = discontinued; }
}
