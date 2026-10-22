package br.com.marealta.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED;

    private static final Map<ReservationStatus, Set<ReservationStatus>> NEXT = new EnumMap<>(ReservationStatus.class);

    static {
        NEXT.put(PENDING, Set.of(CONFIRMED, CANCELLED));
        NEXT.put(CONFIRMED, Set.of(CHECKED_IN, CANCELLED));
        NEXT.put(CHECKED_IN, Set.of(CHECKED_OUT));
        NEXT.put(CHECKED_OUT, Set.of());
        NEXT.put(CANCELLED, Set.of());
    }

    public boolean canMoveTo(ReservationStatus next) {
        return NEXT.get(this).contains(next);
    }

    /** Reserva paga: confirmada, com check-in ou com check-out. */
    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}
