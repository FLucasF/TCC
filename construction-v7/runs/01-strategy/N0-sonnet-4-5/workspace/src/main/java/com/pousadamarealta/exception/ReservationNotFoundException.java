package com.pousadamarealta.exception;

public class ReservationNotFoundException extends ApiException {
    public ReservationNotFoundException() {
        super("RESERVATION_NOT_FOUND", "Reserva não encontrada", 404);
    }
}
