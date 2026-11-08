package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Formatter {
    private static final DateTimeFormatter BR_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("pt", "BR"));
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    public static String formatBrDate(LocalDate date) {
        return date.format(BR_DATE_FORMATTER);
    }

    public static String formatIsoDate(LocalDate date) {
        return date.format(ISO_DATE_FORMATTER);
    }

    public static String formatCurrency(BigDecimal value) {
        return "R$ " + CURRENCY_FORMAT.format(value);
    }

    public static String formatCsvCurrency(BigDecimal value) {
        return String.format("%.2f", value);
    }

    public static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
