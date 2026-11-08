package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    /** Every amount is rounded to cents, half to even, at each step. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** "R$ 1.234,56" */
    public static String brl(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(round(value));
    }

    /** "1234.56", as used in the CSV reports. */
    public static String plain(BigDecimal value) {
        return round(value).toPlainString();
    }
}
