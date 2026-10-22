package br.com.marealta.reservas.domain;

public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    /** Reservas pagas e ainda válidas: têm comprovante e ocupam o quarto. */
    public boolean isPaidAndActive() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}
