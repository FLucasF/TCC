package com.loja.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotificationResponse(
  Long id,
  String type,
  @JsonProperty("orderId")
  Long orderId,
  String status
) {}
