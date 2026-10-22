package com.pousadamarealta.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateParser {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static LocalDate parse(String dateStr) throws IllegalArgumentException {
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyy-MM-dd");
        }
    }

    public static String format(LocalDate date) {
        return date.format(DISPLAY_FORMAT);
    }

    public static String formatForCsv(LocalDate date) {
        return date.toString();
    }
}
