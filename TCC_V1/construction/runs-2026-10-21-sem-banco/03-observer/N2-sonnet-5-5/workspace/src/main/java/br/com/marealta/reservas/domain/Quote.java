package br.com.marealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public static Quote of(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(priceOfNight(room, night));
        }
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    private static BigDecimal priceOfNight(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    /** From December 15 to the end of February, and all of July. */
    static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7
                || (month == 12 && night.getDayOfMonth() >= 15);
    }
}
