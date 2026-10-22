package pousada.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    /** Situações em que o hóspede já pagou e a reserva não foi cancelada. */
    public static final Set<ReservationStatus> PAID = EnumSet.of(CONFIRMED, CHECKED_IN, CHECKED_OUT);
}
