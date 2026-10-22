package br.com.marealta.domain;

import java.time.LocalDate;
import java.time.Month;
import java.math.BigDecimal;

/** High season: from December 15th to the end of February, and all of July. +20% per night. */
public final class HighSeason {
    private static final BigDecimal SURCHARGE = new BigDecimal("1.20");

    private HighSeason() {
    }

    public static boolean contains(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(contains(night) ? rate.multiply(SURCHARGE) : rate);
    }
}
