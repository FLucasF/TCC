package br.com.mare.pousada.domain;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED;

    /** Reserva que tem pagamento: confirmada, com check-in ou com check-out. */
    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }

    public boolean canMoveTo(ReservationStatus target) {
        return switch (target) {
            case PENDING -> false;
            case CONFIRMED -> this == PENDING;
            case CHECKED_IN -> this == CONFIRMED;
            case CHECKED_OUT -> this == CHECKED_IN;
            case CANCELLED -> this == PENDING || this == CONFIRMED;
        };
    }
}
