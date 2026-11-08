package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static Quote of(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
        }
        subtotal = Money.round(subtotal);
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote((int) ChronoUnit.DAYS.between(checkIn, checkOut), subtotal, discount,
                Money.round(subtotal.subtract(discount)));
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        if (Season.isHigh(night)) {
            rate = rate.multiply(Season.HIGH_SEASON_MULTIPLIER);
        }
        return Money.round(rate);
    }
}
