package br.com.marealta.domain;

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

    /** Situações em que a reserva foi paga e não foi cancelada (têm documento e ocupam o quarto). */
    public boolean isPaid() {
        return paid;
    }

    public static List<ReservationStatus> paidOnes() {
        return Arrays.stream(values()).filter(ReservationStatus::isPaid).toList();
    }
}
