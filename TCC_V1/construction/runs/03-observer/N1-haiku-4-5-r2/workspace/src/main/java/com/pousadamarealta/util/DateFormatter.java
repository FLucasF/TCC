package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("0.00");

    public static String format(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    public static String formatCurrency(BigDecimal amount) {
        return "R$ " + CURRENCY_FORMAT.format(amount).replace('.', ',');
    }

    public static String formatIsoDate(LocalDate date) {
        return date.toString();
    }

    public static String formatIsoYearMonth(LocalDate date) {
        return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
    }
}
