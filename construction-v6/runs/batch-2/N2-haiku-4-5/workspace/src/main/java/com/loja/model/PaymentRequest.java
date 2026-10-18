package com.loja.model;

public class PaymentRequest {
    public String transactionId;
    public String paidAt;

    public PaymentRequest() {}

    public PaymentRequest(String transactionId, String paidAt) {
        this.transactionId = transactionId;
        this.paidAt = paidAt;
    }
}
