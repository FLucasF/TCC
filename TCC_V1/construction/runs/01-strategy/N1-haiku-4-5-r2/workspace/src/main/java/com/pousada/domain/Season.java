package com.pousada.domain;

import java.time.LocalDate;
import java.time.Month;

public class Season {
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        if (month == Month.JULY) {
            return true;
        }
        if (month == Month.DECEMBER) {
            return date.getDayOfMonth() >= 15;
        }
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }
        return false;
    }

    public static double getSeasonMultiplier(LocalDate date) {
        return isHighSeason(date) ? 1.20 : 1.0;
    }
}
