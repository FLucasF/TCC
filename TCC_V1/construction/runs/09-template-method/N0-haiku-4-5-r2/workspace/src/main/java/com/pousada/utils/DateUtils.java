package com.pousada.utils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    public static String formatIsoDate(LocalDate date) {
        return date.format(ISO_DATE_FORMATTER);
    }

    public static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, ISO_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
    }

    public static YearMonth parseMonth(String monthStr) {
        try {
            return YearMonth.parse(monthStr, MONTH_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month format: " + monthStr);
        }
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) return true;

        if (month == 12 && day >= 15) return true;

        if (month >= 1 && month <= 2) return true;

        return false;
    }

    public static int daysBetween(LocalDate from, LocalDate to) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(from, to);
    }

    public static boolean startsInMonth(LocalDate date, YearMonth month) {
        return YearMonth.from(date).equals(month);
    }
}
