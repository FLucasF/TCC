package com.pousada.util;

import java.time.LocalDate;
import java.time.YearMonth;

public class DateUtil {
    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month >= 1 && month <= 2) {
            return true;
        }

        return false;
    }

    public static int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    public static String formatDateForCsv(LocalDate date) {
        return date.toString();
    }

    public static String formatYearMonth(YearMonth ym) {
        return ym.toString();
    }
}
