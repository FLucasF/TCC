package com.pousada.service;

public class CapacityExceededException extends RuntimeException {
    public CapacityExceededException(int capacity, int guests) {
        super(String.format("Room capacity (%d) exceeded by %d guests", capacity, guests));
    }
}
