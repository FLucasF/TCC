package com.pousada.util;

import java.time.LocalDate;
import java.time.Month;

public class SeasonUtils {
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && dayOfMonth >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }
}
