package br.com.pousada.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateFormatter {

    private static final DateTimeFormatter PT_BR_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public static String formatPtBr(LocalDate date) {
        return date.format(PT_BR_FORMATTER);
    }

    public static String formatEn(LocalDate date) {
        return date.format(EN_FORMATTER);
    }

    public static String formatIso(LocalDate date) {
        return date.format(ISO_FORMATTER);
    }

    public static LocalDate parseIso(String date) {
        return LocalDate.parse(date, ISO_FORMATTER);
    }
}
