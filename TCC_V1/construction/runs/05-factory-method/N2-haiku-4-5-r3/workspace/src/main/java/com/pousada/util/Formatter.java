package com.pousada.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Formatter {
    private static final DateTimeFormatter BR_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Locale BR_LOCALE = new Locale("pt", "BR");

    public static String formatBrDate(LocalDate date) {
        return date.format(BR_DATE_FORMAT);
    }

    public static String formatIsoDate(LocalDate date) {
        return date.format(ISO_DATE_FORMAT);
    }

    public static String formatEnDate(LocalDate date) {
        return date.format(EN_DATE_FORMAT);
    }

    public static String formatBrCurrency(BigDecimal value) {
        String formatted = String.format(Locale.US, "%.2f", value);
        String[] parts = formatted.split("\\.");
        String intPart = parts[0];
        String decPart = parts[1];
        StringBuilder result = new StringBuilder();
        int len = intPart.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                result.append(".");
            }
            result.append(intPart.charAt(i));
        }
        return "R$ " + result.toString() + "," + decPart;
    }

    public static String formatEnCurrency(BigDecimal value) {
        return String.format(Locale.US, "BRL %.2f", value).replace(",", ".");
    }

    public static String formatCpf(String cpf) {
        if (cpf == null || cpf.isEmpty()) return "";
        cpf = cpf.replaceAll("\\D", "");
        if (cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    public static String formatCnpj(String cnpj) {
        if (cnpj == null || cnpj.isEmpty()) return "";
        cnpj = cnpj.replaceAll("\\D", "");
        if (cnpj.length() != 14) return cnpj;
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." +
               cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }
}
