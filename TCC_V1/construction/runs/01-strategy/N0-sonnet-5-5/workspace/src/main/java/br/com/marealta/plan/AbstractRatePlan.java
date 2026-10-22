package br.com.marealta.plan;

import br.com.marealta.model.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

abstract class AbstractRatePlan implements RatePlan {
    private static final BigDecimal HIGH_SEASON = new BigDecimal("1.20");

    /** Preço de uma noite: diária do quarto, com 20% a mais na alta temporada. */
    static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(Season.isHigh(night) ? rate.multiply(HIGH_SEASON) : rate);
    }

    /** Soma das noites, cada uma já arredondada. */
    static BigDecimal sumOfNights(QuoteRequest r) {
        BigDecimal sum = BigDecimal.ZERO;
        for (LocalDate d = r.checkIn(); d.isBefore(r.checkOut()); d = d.plusDays(1)) {
            sum = sum.add(nightPrice(r.room(), d));
        }
        return Money.round(sum);
    }

    static Quote withDiscount(QuoteRequest r, String fraction) {
        BigDecimal subtotal = sumOfNights(r);
        return new Quote(r.nights(), subtotal, Money.percentOf(subtotal, fraction));
    }

    static BigDecimal full(BigDecimal paid) { return Money.round(paid); }
}
