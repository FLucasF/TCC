package com.loja.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record PaymentRequest(
  @JsonProperty("transactionId")
  String transactionId,
  @JsonProperty("paidAt")
  Instant paidAt
) {}
