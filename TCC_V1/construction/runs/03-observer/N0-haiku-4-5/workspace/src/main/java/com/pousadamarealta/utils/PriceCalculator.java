package com.pousadamarealta.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal PEAK_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static boolean isPeakSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month >= 12 && day >= 15) return true;
        if (month <= 2) return true;
        return false;
    }

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate night) {
        if (isPeakSeason(night)) {
            return baseRate.multiply(PEAK_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
        }
        return baseRate.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightlyRate(baseRate, current));
            current = current.plusDays(1);
        }

        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, boolean isNonRefundable) {
        if (!isNonRefundable) {
            return BigDecimal.ZERO;
        }
        return subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
