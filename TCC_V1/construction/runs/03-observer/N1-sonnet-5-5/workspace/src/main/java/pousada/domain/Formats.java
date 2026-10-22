package pousada.domain;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Como dinheiro e datas aparecem para as pessoas: R$ 1.234,56 e 10/03/2027. */
public final class Formats {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formats() {
    }

    public static String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    public static String date(LocalDate value) {
        return DATE.format(value);
    }
}
