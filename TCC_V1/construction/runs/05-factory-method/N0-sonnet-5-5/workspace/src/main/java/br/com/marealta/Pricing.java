package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

final class Pricing {
    private Pricing() {}

    private static final BigDecimal HIGH_SEASON = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    private static final BigDecimal ISS = new BigDecimal("0.05");

    record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    /** Alta temporada: 15/12 até o fim de fevereiro, e julho inteiro. */
    static boolean isHighSeason(LocalDate night) {
        Month m = night.getMonth();
        return m == Month.JANUARY || m == Month.FEBRUARY || m == Month.JULY
                || (m == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    static BigDecimal nightPrice(BigDecimal rate, LocalDate night) {
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON) : rate);
    }

    /** Uma noite por data de checkIn (inclusive) até checkOut (exclusive). */
    static Quote quote(BigDecimal rate, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.round(BigDecimal.ZERO);
        int nights = 0;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            subtotal = Money.round(subtotal.add(nightPrice(rate, d)));
            nights++;
        }
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? Money.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT))
                : Money.round(BigDecimal.ZERO);
        BigDecimal total = Money.round(subtotal.subtract(discount));
        return new Quote(nights, subtotal, discount, total);
    }

    static BigDecimal iss(BigDecimal total) {
        return Money.round(total.multiply(ISS));
    }
}
