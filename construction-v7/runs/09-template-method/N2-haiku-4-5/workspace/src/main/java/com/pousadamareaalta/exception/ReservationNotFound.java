package com.pousadamareaalta.exception;

public class ReservationNotFound extends ApiException {
    public ReservationNotFound() {
        super("RESERVATION_NOT_FOUND", "Reservation not found", 404);
    }
}
