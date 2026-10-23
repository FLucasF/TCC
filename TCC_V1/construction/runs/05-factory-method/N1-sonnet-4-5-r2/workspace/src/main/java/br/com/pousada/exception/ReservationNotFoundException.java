package br.com.pousada.exception;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long id) {
        super("Reservation not found: " + id);
    }
}
