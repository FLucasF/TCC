package com.pousada.service;

import com.pousada.model.RatePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = BigDecimal.valueOf(1.20);
    private static final BigDecimal DISCOUNT_RATE = BigDecimal.valueOf(0.15);

    public static BigDecimal calculateNightlyPrice(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            BigDecimal highSeasonPrice = baseRate.multiply(HIGH_SEASON_MULTIPLIER);
            return roundHalfToEven(highSeasonPrice);
        }
        return baseRate;
    }

    public static BigDecimal calculateSubtotal(BigDecimal nightlyPrice, int nights) {
        BigDecimal subtotal = nightlyPrice.multiply(BigDecimal.valueOf(nights));
        return roundHalfToEven(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, RatePlan ratePlan) {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            BigDecimal discount = subtotal.multiply(DISCOUNT_RATE);
            return roundHalfToEven(discount);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        BigDecimal total = subtotal.subtract(discount);
        return roundHalfToEven(total);
    }

    public static BigDecimal roundHalfToEven(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // December 15 - February 28/29
        if (month == 12 && day >= 15) return true;
        if (month == 1) return true;
        if (month == 2) return true;

        // All of July
        if (month == 7) return true;

        return false;
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
