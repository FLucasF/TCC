package com.loja.models;

import java.time.Instant;
import java.util.List;

public class Order {
    public String id;
    public Enums.OrderStatus status;
    public Instant createdAt;
    public OrderSummary summary;
    public List<OrderItem> items;
    public String trackingCode;

    public Order() {}

    public Order(String id, Enums.OrderStatus status, Instant createdAt,
                 OrderSummary summary, List<OrderItem> items, String trackingCode) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.summary = summary;
        this.items = items;
        this.trackingCode = trackingCode;
    }
}
