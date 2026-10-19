package com.pousada.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PricingUtils {
    private static final int SCALE = 2;

    public static BigDecimal roundHalfEven(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month == 1 || month == 2) return true;

        return false;
    }

    public static BigDecimal getNightlyRateWithSeason(BigDecimal baseRate, LocalDate date) {
        BigDecimal rate = baseRate;
        if (isHighSeason(date)) {
            rate = rate.multiply(BigDecimal.valueOf(1.2));
        }
        return roundHalfEven(rate);
    }

    public static int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek().getValue() >= 5;
    }

    public static LocalDate getFollowingSunday(LocalDate friday) {
        if (friday.getDayOfWeek().getValue() != 5) {
            throw new IllegalArgumentException("Deve ser uma sexta-feira");
        }
        return friday.plusDays(2);
    }
}
