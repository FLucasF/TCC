package com.loja.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ShipmentRequest {
    @JsonProperty("trackingCode")
    private String trackingCode;
}
