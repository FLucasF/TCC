package com.pousadamarealta.exception;

public class ReservationNotFoundException extends ApiException {
    public ReservationNotFoundException(Long id) {
        super("RESERVATION_NOT_FOUND", "Reservation " + id + " not found", 404);
    }
}
