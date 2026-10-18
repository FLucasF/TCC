package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationDto {
    @JsonProperty
    private String id;

    @JsonProperty
    private String type;

    @JsonProperty
    private String orderId;

    @JsonProperty
    private String status;

    public NotificationDto(String id, String type, String orderId, String status) {
        this.id = id;
        this.type = type;
        this.orderId = orderId;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }
}
