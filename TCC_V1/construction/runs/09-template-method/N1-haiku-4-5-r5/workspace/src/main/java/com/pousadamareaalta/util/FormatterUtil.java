package com.pousadamareaalta.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FormatterUtil {
    private static final DateTimeFormatter BR_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatDateBR(LocalDate date) {
        return date.format(BR_DATE_FORMAT);
    }

    public static String formatDateISO(LocalDate date) {
        return date.format(ISO_DATE_FORMAT);
    }

    public static String formatCurrency(double value) {
        return String.format("R$ %.2f", value).replace(".", ",");
    }

    public static String formatCurrencyCSV(double value) {
        return String.format("%.2f", value);
    }

    public static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11)
        );
    }
}
