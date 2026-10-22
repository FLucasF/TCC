package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public static Quote of(RoomType type, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.round(BigDecimal.ZERO);
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(type, night));
            nights++;
        }
        BigDecimal discount = plan.discount(subtotal);
        return new Quote(nights, subtotal, discount, subtotal.subtract(discount));
    }

    static BigDecimal nightPrice(RoomType type, LocalDate night) {
        BigDecimal rate = type.nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && night.getDayOfMonth() >= 15);
    }
}
