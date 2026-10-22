package pousada.domain;

import java.util.Set;

public enum ReservationStatus {
    PENDING(false),
    CONFIRMED(true),
    CHECKED_IN(true),
    CHECKED_OUT(true),
    CANCELLED(false);

    private final boolean paid;

    ReservationStatus(boolean paid) {
        this.paid = paid;
    }

    /** Reserva paga: confirmada, com check-in ou com check-out. */
    public boolean isPaid() {
        return paid;
    }

    public Set<ReservationStatus> next() {
        return switch (this) {
            case PENDING -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(CHECKED_IN, CANCELLED);
            case CHECKED_IN -> Set.of(CHECKED_OUT);
            case CHECKED_OUT, CANCELLED -> Set.of();
        };
    }
}
