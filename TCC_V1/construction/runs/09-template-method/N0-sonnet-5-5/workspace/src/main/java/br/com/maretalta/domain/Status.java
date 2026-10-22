package br.com.maretalta.domain;

public enum Status {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    /** Reserva com dinheiro pago e não cancelada (confirmada, com check-in ou com check-out). */
    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}
