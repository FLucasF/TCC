package com.pousadamarealta.domain;

import java.time.LocalDate;
import java.time.Month;

public class SeasonCalculator {
    private static final int HIGH_SEASON_START_DAY = 15;

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER && day >= HIGH_SEASON_START_DAY) {
            return true;
        }
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }
        if (month == Month.JULY) {
            return true;
        }
        return false;
    }
}
