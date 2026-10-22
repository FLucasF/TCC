package com.pousada.mare_alta.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class DateUtil {
    private static final LocalDate HIGH_SEASON_START_1 = LocalDate.of(2024, 12, 15);
    private static final LocalDate HIGH_SEASON_END_1 = LocalDate.of(2025, 2, 28);
    private static final int HIGH_SEASON_MONTH = 7;
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal NORMAL_MULTIPLIER = new BigDecimal("1.00");

    public static boolean isHighSeason(LocalDate date) {
        // Check if date is in July
        if (date.getMonthValue() == HIGH_SEASON_MONTH) {
            return true;
        }

        // Check if date is between Dec 15 and Feb 28 (accounting for year changes)
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 12 && day >= 15) {
            return true;
        }
        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    public static BigDecimal getSeasonMultiplier(LocalDate date) {
        return isHighSeason(date) ? HIGH_SEASON_MULTIPLIER : NORMAL_MULTIPLIER;
    }

    public static YearMonth parseYearMonth(String yearMonth) {
        return YearMonth.parse(yearMonth);
    }
}
