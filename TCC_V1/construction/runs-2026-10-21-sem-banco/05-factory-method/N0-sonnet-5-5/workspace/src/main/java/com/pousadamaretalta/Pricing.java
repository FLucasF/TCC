package com.pousadamaretalta;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {}

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    /** Alta temporada: 15/12 até fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        int m = night.getMonthValue();
        return m == 1 || m == 2 || m == 7 || (m == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return isHighSeason(night) ? Money.round(rate.multiply(HIGH_SEASON_FACTOR)) : rate;
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            subtotal = Money.round(subtotal.add(nightPrice(room, d)));
            nights++;
        }
        BigDecimal discount = Money.round(subtotal.multiply(plan.discountRate));
        BigDecimal total = Money.round(subtotal.subtract(discount));
        return new Quote(nights, subtotal, discount, total);
    }
}
