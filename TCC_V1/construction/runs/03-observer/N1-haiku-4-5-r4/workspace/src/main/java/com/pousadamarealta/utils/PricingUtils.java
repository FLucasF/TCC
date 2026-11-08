package com.pousadamarealta.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingUtils {
    private static final BigDecimal SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month >= 1 && month <= 2) return true;

        return false;
    }

    public static BigDecimal calculateNightPrice(BigDecimal nightlyRate, LocalDate date) {
        if (isHighSeason(date)) {
            return roundPrice(nightlyRate.multiply(SEASON_MULTIPLIER));
        }
        return nightlyRate;
    }

    public static BigDecimal roundPrice(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static BigDecimal calculateSubtotal(
        BigDecimal nightlyRate,
        LocalDate checkIn,
        LocalDate checkOut
    ) {
        int nights = calculateNights(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;

        for (int i = 0; i < nights; i++) {
            LocalDate date = checkIn.plusDays(i);
            BigDecimal nightPrice = calculateNightPrice(nightlyRate, date);
            subtotal = subtotal.add(nightPrice);
        }

        return roundPrice(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, boolean isNonRefundable) {
        if (isNonRefundable) {
            return roundPrice(subtotal.multiply(DISCOUNT_RATE));
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return roundPrice(subtotal.subtract(discount));
    }
}
