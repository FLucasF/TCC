package br.com.marealta.pousada.domain;

import java.util.Arrays;
import java.util.List;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED;

    /** Todas as mudanças de situação permitidas, num lugar só. */
    public boolean canMoveTo(ReservationStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == CHECKED_IN || next == CANCELLED;
            case CHECKED_IN -> next == CHECKED_OUT;
            case CHECKED_OUT, CANCELLED -> false;
        };
    }

    /** Reserva paga e ainda válida: confirmada, com check-in ou com check-out. */
    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }

    public static List<ReservationStatus> paid() {
        return Arrays.stream(values()).filter(ReservationStatus::isPaid).toList();
    }
}
