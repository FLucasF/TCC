package br.com.marealta.domain;

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

    /** Diz se uma reserva na situação {@code current} pode passar para esta situação. */
    public boolean canFollow(ReservationStatus current) {
        return switch (this) {
            case PENDING -> false;
            case CONFIRMED -> current == PENDING;
            case CHECKED_IN -> current == CONFIRMED;
            case CHECKED_OUT -> current == CHECKED_IN;
            case CANCELLED -> current == PENDING || current == CONFIRMED;
        };
    }
}
