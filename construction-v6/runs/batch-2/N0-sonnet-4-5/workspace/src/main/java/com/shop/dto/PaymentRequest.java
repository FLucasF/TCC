package com.shop.dto;

import java.time.Instant;

public class PaymentRequest {
    private String transactionId;
    private Instant paidAt;

    public PaymentRequest() {}

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}
