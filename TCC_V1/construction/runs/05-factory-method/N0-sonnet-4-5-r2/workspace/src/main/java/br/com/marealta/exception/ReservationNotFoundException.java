package br.com.marealta.exception;

public class ReservationNotFoundException extends PousadaException {
    public ReservationNotFoundException(Long id) {
        super("RESERVATION_NOT_FOUND", "Reservation " + id + " not found");
    }

    @Override
    public int getHttpStatus() {
        return 404;
    }
}
