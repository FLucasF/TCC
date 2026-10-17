package com.loja.orderservice.dto;

public class ShipmentRequestDTO {
    private String trackingCode;

    public ShipmentRequestDTO() {}

    public ShipmentRequestDTO(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
}
