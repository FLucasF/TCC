package com.marealta;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    private static final Map<ReservationStatus, Set<ReservationStatus>> TRANSITIONS = new EnumMap<>(ReservationStatus.class);

    static {
        TRANSITIONS.put(PENDING, Set.of(CONFIRMED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, Set.of(CHECKED_IN, CANCELLED));
        TRANSITIONS.put(CHECKED_IN, Set.of(CHECKED_OUT));
        TRANSITIONS.put(CHECKED_OUT, Set.of());
        TRANSITIONS.put(CANCELLED, Set.of());
    }

    public boolean canMoveTo(ReservationStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    /** Reserva paga: confirmada, com check-in ou com check-out. */
    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}
