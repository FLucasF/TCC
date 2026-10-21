package com.pousadamareaita.exception;

public class ReservationNotFoundException extends ReservationException {
    public ReservationNotFoundException(int id) {
        super("RESERVATION_NOT_FOUND", "Reservation " + id + " not found");
    }

    @Override
    public int getStatusCode() {
        return 404;
    }
}
