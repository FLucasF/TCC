package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos de texto para pessoas: R$ 1.234,56 e 10/03/2027. */
public final class Format {
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Format() {}

    public static String money(BigDecimal value) {
        return String.format(PT_BR, "R$ %,.2f", value);
    }

    public static String date(LocalDate value) {
        return DATE.format(value);
    }
}
