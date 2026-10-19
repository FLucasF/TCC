package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Alta temporada: 15/12 até o fim de fevereiro e julho inteiro; a noite custa 20% a mais. */
public final class Season {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Season() {}

    public static boolean isHigh(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHigh(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }
}
