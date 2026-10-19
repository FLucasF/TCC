package com.pousadamareaalta.exception;

public class CapacityExceeded extends ApiException {
    public CapacityExceeded() {
        super("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity", 422);
    }
}
