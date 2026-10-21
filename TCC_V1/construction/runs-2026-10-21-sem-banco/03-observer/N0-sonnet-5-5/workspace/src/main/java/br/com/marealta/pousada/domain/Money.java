package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    /** Rounds to cents, half-even. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Formats as "R$ 1.234,56". */
    public static String format(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        df.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + df.format(round(value));
    }
}
