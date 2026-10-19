package br.com.mareal.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Nightly pricing with the high-season surcharge. */
public final class Season {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Season() {}

    public static boolean isHigh(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(RoomType type, LocalDate night) {
        return isHigh(night) ? Money.percent(type.nightlyRate, HIGH_SEASON_FACTOR) : type.nightlyRate;
    }

    /** Sum of the nights in [checkIn, checkOut), each night already rounded. */
    public static BigDecimal sumOfNights(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(nightPrice(type, night));
        }
        return Money.round(sum);
    }
}
