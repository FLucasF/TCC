package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Formatter {
    private static final Locale BRAZIL = new Locale("pt", "BR");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(BRAZIL);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CPF_PATTERN = DateTimeFormatter.ofPattern("000.000.000-00");

    public static String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    public static String formatCpf(String cpf) {
        String digits = cpf.replaceAll("[^0-9]", "");
        if (digits.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
                digits.substring(0, 3),
                digits.substring(3, 6),
                digits.substring(6, 9),
                digits.substring(9, 11));
    }
}
