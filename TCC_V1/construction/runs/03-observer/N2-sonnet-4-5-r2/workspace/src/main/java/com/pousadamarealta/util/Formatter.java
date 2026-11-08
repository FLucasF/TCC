package com.pousadamarealta.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Formatter {
    private static final Locale BRAZIL = new Locale.Builder().setLanguage("pt").setRegion("BR").build();
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(BRAZIL);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String formatMoney(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    public static String formatCpf(String cpf) {
        if (cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." +
               cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" +
               cpf.substring(9, 11);
    }
}
