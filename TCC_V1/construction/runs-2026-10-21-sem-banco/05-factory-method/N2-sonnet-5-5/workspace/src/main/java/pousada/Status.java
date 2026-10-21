package pousada;

public enum Status {
    PENDING(false),
    CONFIRMED(true),
    CHECKED_IN(true),
    CHECKED_OUT(true),
    CANCELLED(false);

    private final boolean paid;

    Status(boolean paid) { this.paid = paid; }

    /** Reserva paga: confirmada, com check-in ou com check-out. */
    public boolean isPaid() { return paid; }

    public boolean holdsRoom() { return this != CANCELLED; }
}
