package com.pousadamareal.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PriceCalculator {
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    public static long round(BigDecimal value) {
        return value.setScale(2, ROUNDING_MODE).multiply(BigDecimal.valueOf(100)).longValue();
    }

    public static long round(double value) {
        return round(BigDecimal.valueOf(value));
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();

        if (month == 7) {
            return true;
        }

        if (month == 12) {
            return date.getDayOfMonth() >= 15;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    public static long applyHighSeasonMultiplier(long priceInCents) {
        return round(BigDecimal.valueOf(priceInCents).divide(BigDecimal.valueOf(100))
            .multiply(BigDecimal.valueOf(1.20)));
    }

    public static long applyDiscount(long baseCents, BigDecimal discountPercentage) {
        return round(BigDecimal.valueOf(baseCents)
            .divide(BigDecimal.valueOf(100))
            .multiply(discountPercentage)
            .divide(BigDecimal.valueOf(100)));
    }

    public static long formatToNonDecimal(long cents) {
        return cents;
    }
}
