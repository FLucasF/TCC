package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month >= 1 && month <= 2) return true;

        return false;
    }

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return round(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightlyRate(baseRate, current));
            current = current.plusDays(1);
        }

        return round(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        }
        return round(BigDecimal.ZERO);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return round(subtotal.subtract(discount));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
