package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Preço das noites: diária do quarto, com acréscimo na alta temporada. */
public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {
    }

    /** 15 de dezembro até o fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        return (month == 12 && night.getDayOfMonth() >= 15) || month == 1 || month == 2 || month == 7;
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    /** Soma das noites de checkIn (inclusive) a checkOut (exclusive). */
    public static BigDecimal subtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(nightPrice(room, night));
        }
        return sum;
    }
}
