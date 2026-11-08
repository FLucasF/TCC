package pousada;

public enum ReservationStatus {
    PENDING(false), CONFIRMED(true), CHECKED_IN(true), CHECKED_OUT(true), CANCELLED(false);

    private final boolean paidStay;

    ReservationStatus(boolean paidStay) {
        this.paidStay = paidStay;
    }

    /** Reserva paga e não cancelada: confirmada, com check-in ou com check-out. */
    public boolean isPaidStay() {
        return paidStay;
    }
}
