package com.loja.orderservice.dto;

import java.math.BigDecimal;

public class ProductResponseDTO {
    private String sku;
    private String name;
    private BigDecimal price;
    private BigDecimal weightKg;
    private Integer stock;
    private Boolean discontinued;

    public ProductResponseDTO() {}

    public ProductResponseDTO(String sku, String name, BigDecimal price, BigDecimal weightKg,
                              Integer stock, Boolean discontinued) {
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

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Boolean getDiscontinued() { return discontinued; }
    public void setDiscontinued(Boolean discontinued) { this.discontinued = discontinued; }
}
