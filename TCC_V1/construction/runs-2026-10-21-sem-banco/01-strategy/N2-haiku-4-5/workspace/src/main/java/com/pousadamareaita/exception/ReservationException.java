package com.pousadamareaita.exception;

public abstract class ReservationException extends RuntimeException {
    private final String errorCode;

    public ReservationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public abstract int getStatusCode();
}
