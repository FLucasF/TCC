package com.pousadamareaita.exception;

public class InvalidRequestException extends ReservationException {
    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", message);
    }

    @Override
    public int getStatusCode() {
        return 400;
    }
}
