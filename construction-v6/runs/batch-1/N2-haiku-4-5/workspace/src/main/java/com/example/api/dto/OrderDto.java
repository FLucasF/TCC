package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public class OrderDto {
    @JsonProperty
    private String id;

    @JsonProperty
    private String status;

    @JsonProperty
    private Instant createdAt;

    @JsonProperty
    private SummaryDto summary;

    @JsonProperty
    private List<OrderItemDto> items;

    @JsonProperty
    private String trackingCode;

    public OrderDto(String id, String status, Instant createdAt, SummaryDto summary, List<OrderItemDto> items, String trackingCode) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.summary = summary;
        this.items = items;
        this.trackingCode = trackingCode;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public SummaryDto getSummary() {
        return summary;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public String getTrackingCode() {
        return trackingCode;
    }
}
