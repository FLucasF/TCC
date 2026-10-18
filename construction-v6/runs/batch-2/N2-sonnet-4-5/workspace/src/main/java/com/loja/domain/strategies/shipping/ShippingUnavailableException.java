package com.loja.domain.strategies.shipping;

public class ShippingUnavailableException extends Exception {
    public ShippingUnavailableException(String message) {
        super(message);
    }

    public ShippingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
