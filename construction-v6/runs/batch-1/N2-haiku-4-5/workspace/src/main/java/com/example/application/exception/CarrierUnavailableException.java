package com.example.application.exception;

public class CarrierUnavailableException extends ApplicationException {
    public CarrierUnavailableException() {
        super("CARRIER_UNAVAILABLE", 503);
    }
}
