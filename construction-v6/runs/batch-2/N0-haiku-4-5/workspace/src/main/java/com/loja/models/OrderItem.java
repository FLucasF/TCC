package com.loja.models;

public class OrderItem {
    public String sku;
    public int quantity;

    public OrderItem(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }
}
