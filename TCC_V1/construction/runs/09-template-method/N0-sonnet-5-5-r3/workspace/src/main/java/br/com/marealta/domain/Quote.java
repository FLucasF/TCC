package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Price of a stay: sum of the nights, discount and total. */
public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public static Quote of(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = Money.round(subtotal.multiply(plan.discountRate()));
        return new Quote(nights, subtotal, discount, subtotal.subtract(discount));
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return isHighSeason(night) ? Money.round(rate.multiply(HIGH_SEASON_FACTOR)) : Money.round(rate);
    }

    /** High season: 15 Dec to the end of February, and all of July. */
    public static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && night.getDayOfMonth() >= 15);
    }
}
