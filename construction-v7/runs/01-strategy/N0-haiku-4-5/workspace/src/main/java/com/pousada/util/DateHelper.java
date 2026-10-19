package com.pousada.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DateHelper {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    public static String formatDateIso(LocalDate date) {
        return date.format(DATE_ISO);
    }

    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_ISO);
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;

        if (month == 12 && day >= 15) return true;

        if (month <= 2) return true;

        return false;
    }

    public static YearMonth parseYearMonth(String yearMonthStr) {
        return YearMonth.parse(yearMonthStr, DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    public static int getDaysInMonth(YearMonth yearMonth) {
        return yearMonth.lengthOfMonth();
    }
}
