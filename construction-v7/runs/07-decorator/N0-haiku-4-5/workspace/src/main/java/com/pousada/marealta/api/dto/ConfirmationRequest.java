package com.pousada.marealta.api.dto;

import java.math.BigDecimal;

public record ConfirmationRequest(
    BigDecimal amountPaid
) {}
