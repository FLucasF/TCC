package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class PaymentRequestDto {
    @JsonProperty
    private String transactionId;

    @JsonProperty
    private Instant paidAt;

    public String getTransactionId() {
        return transactionId;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }
}
