package com.pousadamarealta.util;

public class CpfFormatter {
    public static String format(String cpf) {
        cpf = cpf.replaceAll("[^0-9]", "");
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }
}
