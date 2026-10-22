package br.com.marealta;

import br.com.marealta.Model.RatePlan;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class Pricing {
    private static final BigDecimal HIGH_SEASON = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    private static final BigDecimal ISS = new BigDecimal("0.05");

    private Pricing() {}

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    /** Alta temporada: 15/12 até o fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        int m = night.getMonthValue();
        return m == 7 || m == 1 || m == 2 || (m == 12 && night.getDayOfMonth() >= 15);
    }

    public static Quote quote(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            BigDecimal price = isHighSeason(d) ? Money.round(nightlyRate.multiply(HIGH_SEASON)) : Money.round(nightlyRate);
            subtotal = subtotal.add(price);
            nights++;
        }
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? Money.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT)) : Money.ZERO;
        return new Quote(nights, subtotal, discount, subtotal.subtract(discount));
    }

    public static BigDecimal withheldIss(BigDecimal total) {
        return Money.round(total.multiply(ISS));
    }
}
