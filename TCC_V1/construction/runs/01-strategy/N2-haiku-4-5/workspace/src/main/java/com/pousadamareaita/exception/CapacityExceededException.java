package com.pousadamareaita.exception;

public class CapacityExceededException extends ReservationException {
    public CapacityExceededException() {
        super("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
    }

    @Override
    public int getStatusCode() {
        return 422;
    }
}
