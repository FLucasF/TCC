package com.clothing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShipmentRequest(
    @JsonProperty("trackingCode")
    String trackingCode
) {}
