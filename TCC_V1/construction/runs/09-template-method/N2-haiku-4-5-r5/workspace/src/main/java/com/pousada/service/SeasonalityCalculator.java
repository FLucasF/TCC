package com.pousada.service;

import java.time.LocalDate;

public class SeasonalityCalculator {
    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // July
        if (month == 7) return true;

        // December 15 - February 28/29
        if (month == 12 && day >= 15) return true;
        if (month == 1) return true;
        if (month == 2) return true;

        return false;
    }
}
