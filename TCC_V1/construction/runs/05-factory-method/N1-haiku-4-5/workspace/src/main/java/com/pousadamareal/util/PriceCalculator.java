package com.pousadamareal.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final double HIGH_SEASON_MULTIPLIER = 1.20;
    private static final double DISCOUNT_RATE = 0.15;

    public static double calculateNightlyRate(LocalDate date, double baseRate) {
        if (isHighSeason(date)) {
            return round(baseRate * HIGH_SEASON_MULTIPLIER);
        }
        return baseRate;
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month == 1) return true;
        if (month == 2) return true;
        return false;
    }

    public static double calculateSubtotal(int nights, double nightlyRate) {
        double subtotal = nights * nightlyRate;
        return round(subtotal);
    }

    public static double calculateDiscount(double subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return round(subtotal * DISCOUNT_RATE);
        }
        return 0.0;
    }

    public static double calculateTotal(double subtotal, double discount) {
        return round(subtotal - discount);
    }

    public static double round(double value) {
        BigDecimal bd = new BigDecimal(value);
        return bd.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
