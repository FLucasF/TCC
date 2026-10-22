package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceCalculator {
    private static final int HIGH_SEASON_SURCHARGE_PERCENT = 20;

    public Price calculate(RoomType type, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.zero();
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(type, night));
            nights++;
        }
        BigDecimal discount = Money.percentOf(subtotal, plan.discountPercent());
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private BigDecimal nightPrice(RoomType type, LocalDate night) {
        BigDecimal rate = type.nightlyRate();
        if (!isHighSeason(night)) return Money.round(rate);
        return Money.percentOf(rate, 100 + HIGH_SEASON_SURCHARGE_PERCENT);
    }

    /** 15/12 até fim de fevereiro, e julho inteiro. */
    static boolean isHighSeason(LocalDate day) {
        int month = day.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && day.getDayOfMonth() >= 15);
    }
}
