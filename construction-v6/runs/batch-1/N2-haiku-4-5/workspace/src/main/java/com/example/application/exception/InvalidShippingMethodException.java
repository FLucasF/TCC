package com.example.application.exception;

public class InvalidShippingMethodException extends ApplicationException {
    public InvalidShippingMethodException() {
        super("INVALID_SHIPPING_METHOD", 422);
    }
}
