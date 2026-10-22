package pousada.domain;

import java.util.Arrays;
import java.util.List;

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

    /** Reserva paga e ainda valendo: confirmada, com check-in ou com check-out. */
    public boolean isPaid() {
        return paid;
    }

    public static List<ReservationStatus> paidOnes() {
        return Arrays.stream(values()).filter(ReservationStatus::isPaid).toList();
    }
}
