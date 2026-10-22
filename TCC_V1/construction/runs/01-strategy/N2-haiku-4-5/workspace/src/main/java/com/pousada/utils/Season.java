package com.pousada.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public class Season {
    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month >= 1 && month <= 2) return true;
        if (month == 12 && day >= 15) return true;

        return false;
    }

    public static BigDecimal getSeasonMultiplier(LocalDate date) {
        if (isHighSeason(date)) {
            return BigDecimal.valueOf(1.20);
        }
        return BigDecimal.ONE;
    }
}
