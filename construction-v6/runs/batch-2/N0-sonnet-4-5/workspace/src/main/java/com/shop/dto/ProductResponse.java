package com.shop.dto;

import java.math.BigDecimal;

public class ProductResponse {
    private String sku;
    private String name;
    private BigDecimal price;
    private BigDecimal weightKg;
    private int stock;
    private boolean discontinued;

    public ProductResponse() {}

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
