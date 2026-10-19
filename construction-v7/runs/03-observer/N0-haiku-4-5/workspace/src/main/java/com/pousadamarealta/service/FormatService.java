package com.pousadamarealta.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;

public class FormatService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMAT_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("0.00");

    public static String formatDateBR(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    public static String formatDateISO(LocalDate date) {
        return date.format(DATE_FORMAT_ISO);
    }

    public static String formatCurrency(double value) {
        return String.format("R$ %s", CURRENCY_FORMAT.format(value).replace(".", ","));
    }

    public static String formatCurrencyCSV(double value) {
        return String.format("%.2f", value);
    }

    public static String formatCPF(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11));
    }
}
