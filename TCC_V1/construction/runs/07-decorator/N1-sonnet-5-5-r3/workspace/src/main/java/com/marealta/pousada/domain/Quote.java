package com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public static Quote of(RoomType type, RatePlan plan, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(type, night));
            nights++;
        }
        subtotal = Money.round(subtotal);
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    private static BigDecimal nightPrice(RoomType type, LocalDate night) {
        BigDecimal rate = type.nightlyRate();
        return Money.round(Season.isHigh(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }
}
