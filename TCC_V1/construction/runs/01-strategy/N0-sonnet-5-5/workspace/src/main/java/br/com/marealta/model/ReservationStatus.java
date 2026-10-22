package br.com.marealta.model;

public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    /** Reservas em que o hóspede já pagou (e não cancelou). */
    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}
