package com.loja.orders.shipping.carrier;

public class CarrierUnavailableException extends RuntimeException {

    public CarrierUnavailableException(String message) {
        super(message);
    }

    public CarrierUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
