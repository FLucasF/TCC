package br.com.marealta.pousada.domain;

public enum Status {
    PENDING(false),
    CONFIRMED(true),
    CHECKED_IN(true),
    CHECKED_OUT(true),
    CANCELLED(false);

    private final boolean paid;

    Status(boolean paid) {
        this.paid = paid;
    }

    /** Reservas nesses estados têm o pagamento feito e ainda valendo (comprovante, ocupação). */
    public boolean paid() {
        return paid;
    }
}
