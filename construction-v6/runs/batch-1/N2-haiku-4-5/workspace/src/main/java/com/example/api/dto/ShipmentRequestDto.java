package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShipmentRequestDto {
    @JsonProperty
    private String trackingCode;

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }
}
