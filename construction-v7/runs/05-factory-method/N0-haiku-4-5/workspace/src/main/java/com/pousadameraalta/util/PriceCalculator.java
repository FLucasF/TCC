package com.pousadameraalta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int dayOfMonth = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12) return true;
        if (month == 1) return true;
        if (month == 2 && dayOfMonth <= 28) return true;

        return false;
    }

    public static BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return roundToEvenCents(baseRate.multiply(HIGH_SEASON_MULTIPLIER));
        }
        return roundToEvenCents(baseRate);
    }

    public static BigDecimal calculateSubtotal(BigDecimal nightPrice, int nights) {
        return roundToEvenCents(nightPrice.multiply(BigDecimal.valueOf(nights)));
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, boolean isNonRefundable) {
        if (!isNonRefundable) {
            return BigDecimal.ZERO;
        }
        return roundToEvenCents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE));
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return roundToEvenCents(subtotal.subtract(discount));
    }

    public static BigDecimal calculateIss(BigDecimal total) {
        return roundToEvenCents(total.multiply(new BigDecimal("0.05")));
    }

    public static BigDecimal roundToEvenCents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
