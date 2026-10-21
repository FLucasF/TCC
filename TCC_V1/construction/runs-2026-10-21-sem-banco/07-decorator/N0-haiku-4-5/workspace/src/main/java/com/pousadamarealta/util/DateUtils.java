package com.pousadamarealta.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String formatForDisplay(LocalDate date) {
        return date.format(DISPLAY_FORMAT);
    }

    public static String formatForApi(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;

        if (month == 12 && day >= 15) return true;

        if (month == 1) return true;

        if (month == 2) return true;

        return false;
    }

    public static YearMonth parseMonth(String monthStr) {
        return YearMonth.parse(monthStr);
    }

    public static int daysInRange(LocalDate from, LocalDate to) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(from, to);
    }
}
