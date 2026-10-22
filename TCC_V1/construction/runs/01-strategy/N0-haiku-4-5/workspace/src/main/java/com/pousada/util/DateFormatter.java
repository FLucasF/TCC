package com.pousada.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String formatDisplay(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    public static String formatISO(LocalDate date) {
        return date.format(ISO_FORMAT);
    }

    public static LocalDate parseISO(String date) {
        return LocalDate.parse(date, ISO_FORMAT);
    }
}
