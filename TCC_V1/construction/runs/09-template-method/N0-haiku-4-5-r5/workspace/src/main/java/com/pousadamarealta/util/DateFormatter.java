package com.pousadamarealta.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateFormatter {
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String format(LocalDate date) {
        return date.format(DISPLAY_FORMAT);
    }

    public static String formatISO(LocalDate date) {
        return date.format(ISO_FORMAT);
    }

    public static LocalDate parse(String dateStr) {
        return LocalDate.parse(dateStr, ISO_FORMAT);
    }
}
