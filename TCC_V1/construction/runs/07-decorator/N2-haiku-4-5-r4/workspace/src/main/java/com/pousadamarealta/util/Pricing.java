package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Pricing {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = BigDecimal.valueOf(1.20);
    private static final BigDecimal DISCOUNT_RATE = BigDecimal.valueOf(0.15);
    private static final int SCALE = 2;

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
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER)
                    .setScale(SCALE, RoundingMode.HALF_EVEN);
        }
        return baseRate.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateSubtotal(BigDecimal baseRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightlyRate = calculateNightlyRate(baseRate, current);
            subtotal = subtotal.add(nightlyRate);
            current = current.plusDays(1);
        }

        return subtotal.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return subtotal.multiply(DISCOUNT_RATE)
                    .setScale(SCALE, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount)
                .setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static int getNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) checkIn.until(checkOut).getDays();
    }
}
