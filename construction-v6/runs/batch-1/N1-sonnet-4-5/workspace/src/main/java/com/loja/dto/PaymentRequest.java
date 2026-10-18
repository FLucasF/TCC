package com.loja.dto;

import java.time.Instant;

public record PaymentRequest(
    String transactionId,
    Instant paidAt
) {}
