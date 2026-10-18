package com.loja.dto;

import java.time.LocalDateTime;

public class PaymentRequest {
    private String transactionId;
    private LocalDateTime paidAt;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
