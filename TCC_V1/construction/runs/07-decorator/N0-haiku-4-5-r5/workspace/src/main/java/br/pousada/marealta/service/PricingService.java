package br.pousada.marealta.service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PricingService {
    private static final double HIGH_SEASON_MULTIPLIER = 1.20;
    private static final double NON_REFUNDABLE_DISCOUNT = 0.15;

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;

        if (month == 12 && day >= 15) return true;

        if (month == 1 || month == 2) return true;

        return false;
    }

    public static double calculateNightlyRate(double baseRate) {
        return round(baseRate);
    }

    public static double calculateNightlyRateForDate(double baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return round(baseRate * HIGH_SEASON_MULTIPLIER);
        }
        return round(baseRate);
    }

    public static double calculateSubtotal(double nightlyRate, int nights) {
        return round(nightlyRate * nights);
    }

    public static double calculateDiscount(double subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return round(subtotal * NON_REFUNDABLE_DISCOUNT);
        }
        return 0;
    }

    public static double calculateTotal(double subtotal, double discount) {
        return round(subtotal - discount);
    }

    public static double round(double value) {
        return new BigDecimal(value)
            .setScale(2, RoundingMode.HALF_EVEN)
            .doubleValue();
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
