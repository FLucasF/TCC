package br.com.marealta.dominio;

public enum StatusReserva {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    /** Reservas pagas: confirmada, com check-in ou com check-out. */
    public boolean paga() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }
}
