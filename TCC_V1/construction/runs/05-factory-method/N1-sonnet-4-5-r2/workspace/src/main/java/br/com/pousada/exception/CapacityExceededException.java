package br.com.pousada.exception;

public class CapacityExceededException extends RuntimeException {
    public CapacityExceededException(int guests, int capacity) {
        super("Number of guests (" + guests + ") exceeds room capacity (" + capacity + ")");
    }
}
