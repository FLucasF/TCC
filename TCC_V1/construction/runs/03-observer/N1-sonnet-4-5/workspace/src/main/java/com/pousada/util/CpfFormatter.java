package com.pousada.util;

public class CpfFormatter {
    public static String format(String cpf) {
        String digits = cpf.replaceAll("[^0-9]", "");
        if (digits.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            digits.substring(0, 3),
            digits.substring(3, 6),
            digits.substring(6, 9),
            digits.substring(9, 11)
        );
    }
}
