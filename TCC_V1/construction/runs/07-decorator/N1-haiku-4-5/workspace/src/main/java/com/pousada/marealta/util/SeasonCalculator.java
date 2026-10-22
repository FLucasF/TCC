package com.pousada.marealta.util;

import java.time.LocalDate;
import java.time.Month;

public class SeasonCalculator {
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public static double getNightlyRate(double baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return baseRate * 1.20;
        }
        return baseRate;
    }
}
