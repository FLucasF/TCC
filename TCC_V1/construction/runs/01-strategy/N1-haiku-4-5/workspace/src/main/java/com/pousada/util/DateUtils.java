package com.pousada.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    public static int countNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // July or December 15 - February 28/29
        if (month == 7) return true;
        if (month == 12 && day >= 15) return true;
        if (month >= 1 && month <= 2) return true;
        return false;
    }

    public static boolean isFriday(LocalDate date) {
        return date.getDayOfWeek().getValue() == 5;
    }

    public static boolean isSunday(LocalDate date) {
        return date.getDayOfWeek().getValue() == 7;
    }

    public static LocalDate nextSunday(LocalDate friday) {
        return friday.plusDays(2);
    }

    public static YearMonth getYearMonth(LocalDate date) {
        return YearMonth.from(date);
    }
}
