package br.com.mare.pousada.domain;

import java.math.BigDecimal;

/** Taxa de turismo da prefeitura. O valor muda todo ano: é só aqui que se troca. */
public final class TourismTax {
    private static final BigDecimal PER_GUEST_NIGHT = Money.of("4.50");

    private TourismTax() {
    }

    public static BigDecimal of(Reservation reservation) {
        return Money.round(PER_GUEST_NIGHT.multiply(BigDecimal.valueOf((long) reservation.getGuests() * reservation.getNights())));
    }
}
