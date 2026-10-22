package com.pousadamareaalta.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class SeasonUtil {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        if (month == 7) {
            return true;
        }

        return false;
    }

    public static BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return baseRate.multiply(HIGH_SEASON_MULTIPLIER).setScale(2, RoundingMode.HALF_EVEN);
        }
        return baseRate;
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
