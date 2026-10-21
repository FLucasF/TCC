package com.pousadamareaita.exception;

public class OperationNotAllowedException extends ReservationException {
    public OperationNotAllowedException(String message) {
        super("OPERATION_NOT_ALLOWED", message);
    }

    @Override
    public int getStatusCode() {
        return 409;
    }
}
