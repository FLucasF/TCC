package com.pousada.marealba.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String formatDisplay(LocalDate date) {
        return date.format(DISPLAY_FORMAT);
    }

    public static String formatIso(LocalDate date) {
        return date.format(ISO_FORMAT);
    }

    public static LocalDate parseIso(String date) {
        return LocalDate.parse(date, ISO_FORMAT);
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
