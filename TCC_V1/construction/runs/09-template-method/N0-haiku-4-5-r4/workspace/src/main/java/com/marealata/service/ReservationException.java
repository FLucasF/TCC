package com.marealata.service;

public class ReservationException extends RuntimeException {
    public String errorCode;
    public String message;

    public ReservationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }
}
