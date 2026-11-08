package br.com.marealta.pousada.domain;

public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED;

    /** Reserva cujo pagamento já entrou (e que dá direito ao comprovante). */
    public boolean isPaid() {
        return this == CONFIRMED || this == CHECKED_IN || this == CHECKED_OUT;
    }

    public boolean canMoveTo(ReservationStatus next) {
        return switch (next) {
            case PENDING -> false;
            case CONFIRMED -> this == PENDING;
            case CHECKED_IN -> this == CONFIRMED;
            case CHECKED_OUT -> this == CHECKED_IN;
            case CANCELLED -> this == PENDING || this == CONFIRMED;
        };
    }
}
