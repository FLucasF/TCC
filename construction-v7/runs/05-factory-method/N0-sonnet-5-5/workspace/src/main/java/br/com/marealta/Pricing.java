package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

final class Pricing {
    private static final BigDecimal HIGH_SEASON = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    private Pricing() {}

    record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    /** 15/12 to end of February, and all of July. */
    static boolean highSeason(LocalDate night) {
        int m = night.getMonthValue();
        return m == 1 || m == 2 || m == 7 || (m == 12 && night.getDayOfMonth() >= 15);
    }

    static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return highSeason(night) ? Money.round(rate.multiply(HIGH_SEASON)) : Money.round(rate);
    }

    static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            subtotal = Money.round(subtotal.add(nightPrice(room, d)));
            nights++;
        }
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? Money.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT))
                : Money.ZERO;
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }
}
