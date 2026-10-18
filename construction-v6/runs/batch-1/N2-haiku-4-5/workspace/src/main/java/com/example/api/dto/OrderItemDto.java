package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderItemDto {
    @JsonProperty
    private String sku;

    @JsonProperty
    private Integer quantity;

    public OrderItemDto() {
    }

    public OrderItemDto(String sku, Integer quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() {
        return sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
