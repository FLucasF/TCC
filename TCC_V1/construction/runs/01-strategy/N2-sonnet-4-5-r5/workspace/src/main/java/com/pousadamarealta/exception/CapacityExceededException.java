package com.pousadamarealta.exception;

public class CapacityExceededException extends ApiException {
    public CapacityExceededException(int guests, int capacity) {
        super("CAPACITY_EXCEEDED", "Number of guests (" + guests + ") exceeds room capacity (" + capacity + ")", 422);
    }
}
