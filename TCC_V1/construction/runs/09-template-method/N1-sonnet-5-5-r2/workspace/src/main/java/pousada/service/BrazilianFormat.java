package pousada.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class BrazilianFormat {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private BrazilianFormat() {
    }

    /** R$ 1.234,56 */
    static String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    static String date(LocalDate date) {
        return DATE.format(date);
    }
}
