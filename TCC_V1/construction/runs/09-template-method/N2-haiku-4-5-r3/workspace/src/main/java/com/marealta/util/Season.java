package com.marealta.util;

import java.time.LocalDate;
import java.time.Month;

public class Season {
    private static final int HIGH_SEASON_MARKUP_PERCENT = 20;

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

    public static int getMarkupPercent() {
        return HIGH_SEASON_MARKUP_PERCENT;
    }
}
