package com.pousadamareaalta.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PriceCalculator {
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static double calculatePricePerNight(LocalDate date, double baseRate) {
        if (isHighSeason(date)) {
            return round(baseRate * 1.20);
        }
        return round(baseRate);
    }

    private static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;

        if (month == 12 && day >= 15) return true;

        if (month >= 1 && month <= 2) return true;

        return false;
    }

    public static double calculateSubtotal(LocalDate checkIn, LocalDate checkOut, double baseRate) {
        int nights = calculateNights(checkIn, checkOut);
        double total = BigDecimal.ZERO.doubleValue();

        for (int i = 0; i < nights; i++) {
            LocalDate date = checkIn.plusDays(i);
            double pricePerNight = calculatePricePerNight(date, baseRate);
            total += pricePerNight;
        }

        return round(total);
    }

    public static double calculateDiscount(double subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return round(subtotal * 0.15);
        }
        return 0;
    }

    public static double round(double value) {
        return new BigDecimal(value).setScale(2, ROUNDING_MODE).doubleValue();
    }
}
