package com.loja.model;

public class OrderItem {
    public String sku;
    public Integer quantity;

    public OrderItem() {}

    public OrderItem(String sku, Integer quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }
}
