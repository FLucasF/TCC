package br.com.marealta.reservas.domain;

public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    /** Reservas com pagamento feito e que não foram canceladas. */
    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}
