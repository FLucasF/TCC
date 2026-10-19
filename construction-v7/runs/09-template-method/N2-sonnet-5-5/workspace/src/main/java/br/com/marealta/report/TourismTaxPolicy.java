package br.com.marealta.report;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;

/** Taxa da prefeitura por hóspede por noite. O valor muda todo ano: é só trocar aqui. */
public class TourismTaxPolicy {
    public static final BigDecimal CURRENT_RATE = Money.of("4.50");

    private final BigDecimal ratePerGuestNight;

    public TourismTaxPolicy(BigDecimal ratePerGuestNight) { this.ratePerGuestNight = ratePerGuestNight; }

    public static TourismTaxPolicy current() { return new TourismTaxPolicy(CURRENT_RATE); }

    public BigDecimal taxFor(Reservation r) {
        return Money.round(ratePerGuestNight.multiply(BigDecimal.valueOf((long) r.guests() * r.nights())));
    }
}
