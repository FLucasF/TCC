package com.clothing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentRequest(
    @JsonProperty("transactionId")
    String transactionId,
    @JsonProperty("paidAt")
    String paidAt
) {}
