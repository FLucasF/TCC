package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ProductDto {
    @JsonProperty
    private String sku;

    @JsonProperty
    private String name;

    @JsonProperty
    private BigDecimal price;

    @JsonProperty
    private BigDecimal weightKg;

    @JsonProperty
    private Integer stock;

    @JsonProperty
    private Boolean discontinued;

    public ProductDto(String sku, String name, BigDecimal price, BigDecimal weightKg, Integer stock, Boolean discontinued) {
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

    public Boolean getDiscontinued() {
        return discontinued;
    }
}
