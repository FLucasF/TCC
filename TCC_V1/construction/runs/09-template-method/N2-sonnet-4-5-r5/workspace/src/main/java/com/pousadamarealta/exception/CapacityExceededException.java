package com.pousadamarealta.exception;

public class CapacityExceededException extends ApiException {
    public CapacityExceededException(String message) {
        super("CAPACITY_EXCEEDED", message, 422);
    }
}
