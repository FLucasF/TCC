package com.pousada.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static class PriceBreakdown {
        public final BigDecimal nightPrice;
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public PriceBreakdown(BigDecimal nightPrice, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.nightPrice = nightPrice;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    private static BigDecimal roundHalfUp(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean isHighSeason(LocalDate date) {
        YearMonth yearMonth = YearMonth.from(date);
        int month = date.getMonthValue();

        if (month >= 12 || month <= 2) return true;
        if (month == 7) return true;
        return false;
    }

    public static PriceBreakdown calculate(BigDecimal baseNightlyRate, LocalDate checkIn, LocalDate checkOut, String ratePlan) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal nightPrice = baseNightlyRate;
        LocalDate current = checkIn;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            BigDecimal price = nightPrice;
            if (isHighSeason(current)) {
                price = price.multiply(HIGH_SEASON_MULTIPLIER);
            }
            price = roundHalfUp(price);
            subtotal = subtotal.add(price);
            current = current.plusDays(1);
        }

        subtotal = roundHalfUp(subtotal);

        BigDecimal discount;
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            discount = subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE);
            discount = roundHalfUp(discount);
        } else {
            discount = roundHalfUp(BigDecimal.ZERO);
        }

        BigDecimal total = subtotal.subtract(discount);
        total = roundHalfUp(total);

        return new PriceBreakdown(nightPrice, subtotal, discount, total);
    }
}
