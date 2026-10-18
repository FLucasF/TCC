package com.shop.dto;

import com.shop.model.OrderItem;
import com.shop.model.enums.OrderStatus;
import java.time.Instant;
import java.util.List;

public class OrderResponse {
    private Long id;
    private OrderStatus status;
    private Instant createdAt;
    private CheckoutSummary summary;
    private List<OrderItem> items;
    private String trackingCode;

    public OrderResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public CheckoutSummary getSummary() { return summary; }
    public void setSummary(CheckoutSummary summary) { this.summary = summary; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
}
