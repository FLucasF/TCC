package com.pousada.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateFormatter {
    private static final DateTimeFormatter BR_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String formatBR(LocalDate date) {
        return date.format(BR_FORMAT);
    }

    public static String formatISO(LocalDate date) {
        return date.format(ISO_FORMAT);
    }

    public static LocalDate parseISO(String date) {
        return LocalDate.parse(date, ISO_FORMAT);
    }
}
