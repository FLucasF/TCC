package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {}

    static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }
}
