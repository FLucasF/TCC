package com.pousada.domain.pricing;

import java.time.LocalDate;
import java.time.Month;

public class SeasonCalculator {

    public static boolean isHighSeason(LocalDate date) {
        int day = date.getDayOfMonth();
        Month month = date.getMonth();

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
}
