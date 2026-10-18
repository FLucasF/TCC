package com.example.application.exception;

public class ShippingMethodUnavailableException extends ApplicationException {
    public ShippingMethodUnavailableException() {
        super("SHIPPING_METHOD_UNAVAILABLE", 422);
    }
}
