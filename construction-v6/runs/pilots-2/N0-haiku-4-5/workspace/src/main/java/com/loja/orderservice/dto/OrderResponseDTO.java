package com.loja.orderservice.dto;

import com.loja.orderservice.entity.OrderStatus;
import com.loja.orderservice.entity.OrderSummary;
import java.time.Instant;
import java.util.List;

public class OrderResponseDTO {
    private String id;
    private OrderStatus status;
    private Instant createdAt;
    private OrderSummary summary;
    private List<OrderItemDTO> items;
    private String trackingCode;

    public OrderResponseDTO() {}

    public OrderResponseDTO(String id, OrderStatus status, Instant createdAt, OrderSummary summary,
                            List<OrderItemDTO> items, String trackingCode) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.summary = summary;
        this.items = items;
        this.trackingCode = trackingCode;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public OrderSummary getSummary() { return summary; }
    public void setSummary(OrderSummary summary) { this.summary = summary; }

    public List<OrderItemDTO> getItems() { return items; }
    public void setItems(List<OrderItemDTO> items) { this.items = items; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
}
