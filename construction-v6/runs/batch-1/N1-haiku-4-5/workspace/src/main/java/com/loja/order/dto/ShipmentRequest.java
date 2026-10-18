package com.loja.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShipmentRequest(
  @JsonProperty("trackingCode")
  String trackingCode
) {}
