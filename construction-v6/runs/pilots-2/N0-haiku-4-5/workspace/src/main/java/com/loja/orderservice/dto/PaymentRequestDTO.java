package com.loja.orderservice.dto;

import java.time.Instant;

public class PaymentRequestDTO {
    private String transactionId;
    private Instant paidAt;

    public PaymentRequestDTO() {}

    public PaymentRequestDTO(String transactionId, Instant paidAt) {
        this.transactionId = transactionId;
        this.paidAt = paidAt;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
