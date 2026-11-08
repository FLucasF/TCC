package com.pousada.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingUtils {
    private static final BigDecimal PEAK_SEASON_MULTIPLIER = BigDecimal.valueOf(1.20);
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = BigDecimal.valueOf(0.15);

    public static boolean isPeakSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }
        if (month == Month.DECEMBER) {
            return day >= 15;
        }
        return month == Month.JANUARY || month == Month.FEBRUARY;
    }

    public static BigDecimal roundToNearest(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isPeakSeason(date)) {
            return roundToNearest(baseRate.multiply(PEAK_SEASON_MULTIPLIER));
        }
        return baseRate;
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightlyRate(baseRate, current));
            current = current.plusDays(1);
        }
        return subtotal;
    }

    public static BigDecimal calculateDiscount(String ratePlan, BigDecimal subtotal) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return roundToNearest(subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE));
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return roundToNearest(subtotal.subtract(discount));
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
