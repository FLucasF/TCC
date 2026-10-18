package com.example.application.exception;

public class PaymentMethodUnavailableException extends ApplicationException {
    public PaymentMethodUnavailableException() {
        super("PAYMENT_METHOD_UNAVAILABLE", 422);
    }
}
