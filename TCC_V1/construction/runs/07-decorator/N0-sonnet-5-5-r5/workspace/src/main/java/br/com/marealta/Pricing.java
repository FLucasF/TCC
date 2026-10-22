package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Pricing {
    private static final BigDecimal HIGH_SEASON = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    private Pricing() {}

    /** 15/12 até o fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        int m = night.getMonthValue();
        return m == 1 || m == 2 || m == 7 || (m == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(BigDecimal rate, LocalDate night) {
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON) : rate);
    }

    public static Quote quote(BigDecimal rate, LocalDate checkIn, LocalDate checkOut, String ratePlan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            subtotal = Money.round(subtotal.add(nightPrice(rate, d)));
            nights++;
        }
        BigDecimal discount = ratePlan.equals("NON_REFUNDABLE")
                ? Money.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT))
                : Money.ZERO;
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }
}
