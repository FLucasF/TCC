package com.loja.model;

import java.time.Instant;
import java.util.List;

public class Order {
    public String id;
    public String cpf;
    public String email;
    public OrderStatus status;
    public Instant createdAt;
    public Instant paidAt;
    public OrderSummary summary;
    public List<OrderItem> items;
    public String trackingCode;

    public Order() {}

    public Order(String id, String cpf, String email, OrderStatus status, Instant createdAt,
                 OrderSummary summary, List<OrderItem> items) {
        this.id = id;
        this.cpf = cpf;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
        this.summary = summary;
        this.items = items;
        this.trackingCode = null;
    }
}
