package pousada.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    private static final Map<ReservationStatus, Set<ReservationStatus>> NEXT = new EnumMap<>(ReservationStatus.class);

    static {
        NEXT.put(PENDING, EnumSet.of(CONFIRMED, CANCELLED));
        NEXT.put(CONFIRMED, EnumSet.of(CHECKED_IN, CANCELLED));
        NEXT.put(CHECKED_IN, EnumSet.of(CHECKED_OUT));
        NEXT.put(CHECKED_OUT, EnumSet.noneOf(ReservationStatus.class));
        NEXT.put(CANCELLED, EnumSet.noneOf(ReservationStatus.class));
    }

    public boolean canGoTo(ReservationStatus next) {
        return NEXT.get(this).contains(next);
    }

    /** Confirmada, com check-in ou com check-out: há pagamento. */
    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}
