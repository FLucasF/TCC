package com.pousadamarealta.pricing;

import java.time.LocalDate;
import java.time.Month;

public class Season {
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER) {
            return day >= 15;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return day <= 28;
        }

        return false;
    }
}
