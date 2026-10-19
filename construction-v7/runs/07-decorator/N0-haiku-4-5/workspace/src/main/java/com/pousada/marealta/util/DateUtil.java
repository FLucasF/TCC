package com.pousada.marealta.util;

import java.time.LocalDate;

public class DateUtil {
    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12) {
            return day >= 15;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }
}
