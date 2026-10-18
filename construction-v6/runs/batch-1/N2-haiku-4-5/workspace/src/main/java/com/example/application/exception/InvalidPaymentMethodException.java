package com.example.application.exception;

public class InvalidPaymentMethodException extends ApplicationException {
    public InvalidPaymentMethodException() {
        super("INVALID_PAYMENT_METHOD", 422);
    }
}
