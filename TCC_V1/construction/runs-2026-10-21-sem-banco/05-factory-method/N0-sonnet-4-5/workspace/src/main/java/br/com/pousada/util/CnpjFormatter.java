package br.com.pousada.util;

public class CnpjFormatter {

    public static String format(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + "." +
               cnpj.substring(2, 5) + "." +
               cnpj.substring(5, 8) + "/" +
               cnpj.substring(8, 12) + "-" +
               cnpj.substring(12, 14);
    }
}
