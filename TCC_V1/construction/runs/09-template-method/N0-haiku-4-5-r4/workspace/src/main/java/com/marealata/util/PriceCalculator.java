package com.marealata.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final BigDecimal NIGHTLY_RATE_STANDARD_101 = new BigDecimal("249.90");
    private static final BigDecimal NIGHTLY_RATE_STANDARD_102 = new BigDecimal("249.90");
    private static final BigDecimal NIGHTLY_RATE_DELUXE_201 = new BigDecimal("389.90");
    private static final BigDecimal NIGHTLY_RATE_DELUXE_202 = new BigDecimal("389.90");
    private static final BigDecimal NIGHTLY_RATE_SUITE_301 = new BigDecimal("579.90");

    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT_RATE = new BigDecimal("0.15");

    public static BigDecimal getNightlyRate(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> NIGHTLY_RATE_STANDARD_101;
            case 201, 202 -> NIGHTLY_RATE_DELUXE_201;
            case 301 -> NIGHTLY_RATE_SUITE_301;
            default -> throw new IllegalArgumentException("Invalid room number: " + roomNumber);
        };
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int dayOfMonth = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12) return true;
        if (month == 1) return true;
        if (month == 2) return true;

        return false;
    }

    public static BigDecimal calculateNightlyPrice(int roomNumber, LocalDate date) {
        BigDecimal baseRate = getNightlyRate(roomNumber);
        if (isHighSeason(date)) {
            baseRate = baseRate.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return round(baseRate);
    }

    public static BigDecimal calculateSubtotal(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightlyPrice(roomNumber, current));
            current = current.plusDays(1);
        }
        return round(subtotal);
    }

    public static BigDecimal calculateDiscount(BigDecimal subtotal, com.marealata.model.RatePlan ratePlan) {
        if (ratePlan == com.marealata.model.RatePlan.NON_REFUNDABLE) {
            return round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT_RATE));
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal discount) {
        return round(subtotal.subtract(discount));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
