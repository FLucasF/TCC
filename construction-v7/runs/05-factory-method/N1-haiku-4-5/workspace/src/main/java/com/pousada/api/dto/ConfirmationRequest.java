package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ConfirmationRequest {
    @JsonProperty("amountPaid")
    public BigDecimal amountPaid;
}
