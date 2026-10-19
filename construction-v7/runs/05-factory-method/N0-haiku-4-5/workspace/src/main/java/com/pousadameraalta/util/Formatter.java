package com.pousadameraalta.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Formatter {
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatPtDate(LocalDate date) {
        return date.format(PT_DATE_FORMAT);
    }

    public static String formatIsoDate(LocalDate date) {
        return date.format(ISO_DATE_FORMAT);
    }

    public static String formatEnDate(LocalDate date) {
        return date.format(EN_DATE_FORMAT);
    }

    public static String formatBrl(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    public static String formatBrlCsv(BigDecimal amount) {
        return String.format("%.2f", amount);
    }

    public static String formatBrlEnglish(BigDecimal amount) {
        return String.format("BRL %,.2f", amount);
    }

    public static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    public static String formatCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) return cnpj;
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/" +
               cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }
}
