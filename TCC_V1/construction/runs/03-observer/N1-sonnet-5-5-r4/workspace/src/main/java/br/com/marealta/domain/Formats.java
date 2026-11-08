package br.com.marealta.domain;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Como dinheiro e datas aparecem nos textos para as pessoas: R$ 1.234,56 e 10/03/2027. */
public final class Formats {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.of("pt", "BR"));

    private Formats() {
    }

    public static String money(BigDecimal value) {
        return "R$ " + new DecimalFormat("#,##0.00", SYMBOLS).format(value);
    }

    public static String date(LocalDate date) {
        return DATE.format(date);
    }
}
