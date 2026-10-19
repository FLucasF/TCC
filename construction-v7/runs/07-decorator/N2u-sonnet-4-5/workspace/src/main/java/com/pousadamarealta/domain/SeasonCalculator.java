package com.pousadamarealta.domain;

import java.time.LocalDate;
import java.time.Month;

public class SeasonCalculator {
    private static final double HIGH_SEASON_MULTIPLIER = 1.20;

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        // Julho inteiro
        if (month == Month.JULY) {
            return true;
        }

        // 15 de dezembro até o fim de fevereiro
        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public static double getSeasonMultiplier(LocalDate date) {
        return isHighSeason(date) ? HIGH_SEASON_MULTIPLIER : 1.0;
    }
}
