package com.pousada.marealta.util;

import java.time.LocalDate;
import java.time.Month;

public class SeasonHelper {
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) return true;

        if (month == Month.DECEMBER) return day >= 15;

        if (month == Month.JANUARY || month == Month.FEBRUARY) return true;

        return false;
    }
}
