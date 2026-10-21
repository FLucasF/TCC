package com.pousadamareaita.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

public class PricingUtils {
    public static BigDecimal roundBanker(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int dayOfMonth = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12) {
            return dayOfMonth >= 15;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    public static BigDecimal getNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return roundBanker(baseRate.multiply(BigDecimal.valueOf(1.20)));
        }
        return baseRate;
    }

    public static int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
