package com.pousada.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class PriceCalculator {
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12) {
            return day >= 15;
        }

        if (month <= 2) {
            return true;
        }

        return false;
    }

    public static BigDecimal roundPrice(BigDecimal value) {
        return value.setScale(2, ROUNDING);
    }

    public static BigDecimal applySeasonalAdjustment(BigDecimal nightlyRate, LocalDate date) {
        if (isHighSeason(date)) {
            return roundPrice(nightlyRate.multiply(BigDecimal.valueOf(1.20)));
        }
        return nightlyRate;
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static boolean isWeekend(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        return dayOfWeek == 5 || dayOfWeek == 6 || dayOfWeek == 7;
    }

    public static boolean isWeekendPackageValid(LocalDate checkIn, LocalDate checkOut) {
        int days = calculateNights(checkIn, checkOut);
        if (days != 2) {
            return false;
        }

        int checkInDow = checkIn.getDayOfWeek().getValue();
        int checkOutDow = checkOut.getDayOfWeek().getValue();

        return checkInDow == 5 && checkOutDow == 7;
    }
}
