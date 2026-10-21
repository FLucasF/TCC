package com.pousadamarealta.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

public class DateUtil {
    public static int daysBetween(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end);
    }

    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        // July
        if (month == Month.JULY) {
            return true;
        }

        // December 15 to end
        if (month == Month.DECEMBER && dayOfMonth >= 15) {
            return true;
        }

        // January to February end
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public static String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public static String formatDateISO(LocalDate date) {
        return date.toString();
    }
}
