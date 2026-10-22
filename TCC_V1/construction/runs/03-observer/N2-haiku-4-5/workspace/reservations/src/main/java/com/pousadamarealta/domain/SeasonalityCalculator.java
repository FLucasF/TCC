package com.pousadamarealta.domain;

import java.time.LocalDate;
import java.time.Month;

public class SeasonalityCalculator {
    private static final double HIGH_SEASON_MULTIPLIER = 1.20;

    public static Money calculateNightlyRate(double baseRate, LocalDate night) {
        if (isHighSeason(night)) {
            return Money.of(baseRate * HIGH_SEASON_MULTIPLIER);
        }
        return Money.of(baseRate);
    }

    private static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && dayOfMonth >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }
}
