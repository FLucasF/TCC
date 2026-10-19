package com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cálculo da soma das noites, com acréscimo de alta temporada. */
public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {}

    /** Alta temporada: 15/12 a fim de fevereiro e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.type().nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    public static BigDecimal subtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = Money.round(sum.add(nightPrice(room, night)));
        }
        return sum;
    }
}
