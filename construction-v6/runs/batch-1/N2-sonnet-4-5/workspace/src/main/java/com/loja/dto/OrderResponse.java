package com.loja.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {
    private String id;
    private String status;
    private LocalDateTime createdAt;
    private SummaryResponse summary;
    private List<ItemRequest> items;
    private String trackingCode;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public SummaryResponse getSummary() { return summary; }
    public void setSummary(SummaryResponse summary) { this.summary = summary; }

    public List<ItemRequest> getItems() { return items; }
    public void setItems(List<ItemRequest> items) { this.items = items; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
}
