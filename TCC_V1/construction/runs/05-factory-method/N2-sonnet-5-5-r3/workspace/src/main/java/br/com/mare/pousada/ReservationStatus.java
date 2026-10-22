package br.com.mare.pousada;

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
}
