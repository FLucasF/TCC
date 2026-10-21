package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Season;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Cálculo por diária, compartilhado pelos planos que cobram noite a noite. */
final class NightlyPricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private NightlyPricing() {}

    static BigDecimal nightPrice(BigDecimal rate, LocalDate night) {
        return Season.isHigh(night) ? Money.round(rate.multiply(HIGH_SEASON_FACTOR)) : Money.round(rate);
    }

    static BigDecimal sumOfNights(BookingRequest r) {
        BigDecimal sum = Money.ZERO;
        for (LocalDate d = r.checkIn(); d.isBefore(r.checkOut()); d = d.plusDays(1)) {
            sum = sum.add(nightPrice(r.room().nightlyRate(), d));
        }
        return sum;
    }

    static Quote withPercentDiscount(BookingRequest r, int percent) {
        BigDecimal subtotal = sumOfNights(r);
        return Quote.of(r.nights(), subtotal, Money.percent(subtotal, percent));
    }
}
