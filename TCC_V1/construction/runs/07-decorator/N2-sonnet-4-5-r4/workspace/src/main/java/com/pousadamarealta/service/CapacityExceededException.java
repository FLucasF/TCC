package com.pousadamarealta.service;

public class CapacityExceededException extends RuntimeException {
    public CapacityExceededException() {
        super("Number of guests exceeds room capacity");
    }
}
