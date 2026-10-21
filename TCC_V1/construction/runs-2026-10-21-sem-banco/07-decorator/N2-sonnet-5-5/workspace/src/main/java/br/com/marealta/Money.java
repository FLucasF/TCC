package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class Money {
    static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Formato do comprovante: R$ 1.234,56. */
    static String brl(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(value);
    }
}
