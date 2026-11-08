package com.pousada.domain;

import java.time.LocalDate;
import java.time.Month;

public class SeasonCalculator {
    private static final double HIGH_SEASON_MULTIPLIER = 1.20;

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if ((month == 12 && day >= 15) || month == 1 || month == 2) {
            return true;
        }
        if (month == 7) {
            return true;
        }
        return false;
    }

    public static double calculateNightlyRate(double baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return Rounding.round(baseRate * HIGH_SEASON_MULTIPLIER);
        }
        return Rounding.round(baseRate);
    }

    public static double calculateSubtotal(double baseRate, LocalDate checkIn, LocalDate checkOut) {
        double subtotal = 0.0;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            subtotal += calculateNightlyRate(baseRate, current);
            current = current.plusDays(1);
        }

        return Rounding.round(subtotal);
    }
}
