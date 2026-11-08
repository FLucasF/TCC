package br.com.marealta.pricing;

import java.time.LocalDate;
import java.time.Month;

public class SeasonCalculator {
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        return false;
    }
}
