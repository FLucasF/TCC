package com.pousadamarealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = BigDecimal.valueOf(1.20);
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static class PriceResult {
        public BigDecimal subtotal;
        public BigDecimal discount;
        public BigDecimal total;

        public PriceResult(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }

    private static BigDecimal roundHalfEven(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month >= 1 && month <= 2) return true;

        return false;
    }

    public static BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate night) {
        if (isHighSeason(night)) {
            return roundHalfEven(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return roundHalfEven(baseRate);
    }

    public static PriceResult calculate(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut, String ratePlan) {
        long nights = checkOut.toEpochDay() - checkIn.toEpochDay();

        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal nightPrice = calculateNightlyPrice(baseRate, current);
            subtotal = subtotal.add(nightPrice);
            current = current.plusDays(1);
        }
        subtotal = roundHalfEven(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            discount = roundHalfEven(subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE));
        }

        BigDecimal total = roundHalfEven(subtotal.subtract(discount));

        return new PriceResult(subtotal, discount, total);
    }
}
