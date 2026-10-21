package com.pousada.marealta.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FormatHelper {
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String formatIsoDate(LocalDate date) {
        return date.format(ISO_DATE);
    }

    public static String formatBrDate(LocalDate date) {
        return date.format(BR_DATE);
    }

    public static String formatCurrency(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", "#TEMP#").replace(".", ",").replace("#TEMP#", ".");
    }

    public static String formatCurrencyCsv(BigDecimal value) {
        return value.toPlainString();
    }

    public static LocalDate parseIsoDate(String date) {
        return LocalDate.parse(date, ISO_DATE);
    }
}
