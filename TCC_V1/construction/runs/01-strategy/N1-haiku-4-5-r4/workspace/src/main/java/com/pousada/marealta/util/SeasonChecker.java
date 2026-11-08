package com.pousada.marealta.util;

import java.time.LocalDate;
import java.time.Month;

public class SeasonChecker {
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER) {
            return dayOfMonth >= 15;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }
}
