package com.pousada.exception;

public class CapacityExceededException extends RuntimeException {
    public CapacityExceededException() {
        super("Número de hóspedes excede a capacidade do quarto");
    }
}
