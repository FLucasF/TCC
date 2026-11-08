package br.com.marealta.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    /** Statuses of reservations that were paid for (and not cancelled). */
    public static final Set<ReservationStatus> PAID = EnumSet.of(CONFIRMED, CHECKED_IN, CHECKED_OUT);

    /** Statuses that hold the room (everything but cancelled). */
    public static final Set<ReservationStatus> ACTIVE = EnumSet.complementOf(EnumSet.of(CANCELLED));
}
