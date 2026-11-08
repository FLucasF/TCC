package com.pousadamarealta.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String formatDisplay(LocalDate date) {
        return date.format(DISPLAY_FORMATTER);
    }

    public static String formatISO(LocalDate date) {
        return date.format(ISO_FORMATTER);
    }

    public static LocalDate parseISO(String date) {
        return LocalDate.parse(date, ISO_FORMATTER);
    }

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 7) {
            return true;
        }

        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }
}
