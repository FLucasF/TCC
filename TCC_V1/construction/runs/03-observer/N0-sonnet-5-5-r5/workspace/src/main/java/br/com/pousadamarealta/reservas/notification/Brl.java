package br.com.pousadamarealta.reservas.notification;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatação para texto: R$ 1.234,56 e 10/03/2027. */
public final class Brl {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private Brl() {}

    public static String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    public static String date(LocalDate date) {
        return DATE.format(date);
    }

    /** 11 dígitos viram 111.222.333-44; qualquer outra coisa fica como foi informada. */
    public static String cpf(String cpf) {
        if (cpf.matches("\\d{11}")) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
