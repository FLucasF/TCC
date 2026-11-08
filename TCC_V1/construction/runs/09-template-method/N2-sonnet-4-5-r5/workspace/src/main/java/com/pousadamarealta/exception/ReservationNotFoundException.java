package com.pousadamarealta.exception;

public class ReservationNotFoundException extends ApiException {
    public ReservationNotFoundException(String message) {
        super("RESERVATION_NOT_FOUND", message, 404);
    }
}
