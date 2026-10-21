package com.pousadamarealta.service;

import java.time.LocalDate;
import java.time.YearMonth;

public class PricingService {
    private static final double HIGH_SEASON_MULTIPLIER = 1.20;
    private static final double NON_REFUNDABLE_DISCOUNT_RATE = 0.15;

    public double calculateNightlyRate(double baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return roundHalfToEven(baseRate * HIGH_SEASON_MULTIPLIER);
        }
        return baseRate;
    }

    public double calculateSubtotal(double nightlyRate, int nights) {
        return roundHalfToEven(nightlyRate * nights);
    }

    public double calculateDiscount(double subtotal, String ratePlan) {
        if ("NON_REFUNDABLE".equals(ratePlan)) {
            return roundHalfToEven(subtotal * NON_REFUNDABLE_DISCOUNT_RATE);
        }
        return 0.0;
    }

    public double calculateTotal(double subtotal, double discount) {
        return roundHalfToEven(subtotal - discount);
    }

    private boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int dayOfMonth = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 && dayOfMonth >= 15) {
            return true;
        }

        if (month >= 1 && month <= 2) {
            return true;
        }

        return false;
    }

    private double roundHalfToEven(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
