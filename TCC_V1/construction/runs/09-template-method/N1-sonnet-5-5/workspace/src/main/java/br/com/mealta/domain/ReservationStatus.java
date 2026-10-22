package br.com.mealta.domain;

import java.util.Set;

public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    public boolean canGoTo(ReservationStatus next) {
        return allowedNext().contains(next);
    }

    /** Reserva que já foi paga e não foi cancelada. */
    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }

    private Set<ReservationStatus> allowedNext() {
        return switch (this) {
            case PENDING -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(CHECKED_IN, CANCELLED);
            case CHECKED_IN -> Set.of(CHECKED_OUT);
            case CHECKED_OUT, CANCELLED -> Set.of();
        };
    }
}
