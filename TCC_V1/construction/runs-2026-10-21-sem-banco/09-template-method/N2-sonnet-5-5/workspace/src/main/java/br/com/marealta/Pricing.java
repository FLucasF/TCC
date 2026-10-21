package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {}

    public static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(RoomType type, LocalDate night) {
        BigDecimal rate = type.nightlyRate();
        return isHighSeason(night) ? Money.round(rate.multiply(HIGH_SEASON_FACTOR)) : rate;
    }

    public static BigDecimal subtotal(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(nightPrice(type, night));
        }
        return sum;
    }
}
