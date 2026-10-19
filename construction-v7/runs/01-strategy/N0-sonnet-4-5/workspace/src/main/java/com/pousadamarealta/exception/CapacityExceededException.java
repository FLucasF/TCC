package com.pousadamarealta.exception;

public class CapacityExceededException extends ApiException {
    public CapacityExceededException() {
        super("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade do quarto", 422);
    }
}
