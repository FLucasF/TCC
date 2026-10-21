package com.pousada.exception;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException() {
        super("Reserva não encontrada");
    }
}
