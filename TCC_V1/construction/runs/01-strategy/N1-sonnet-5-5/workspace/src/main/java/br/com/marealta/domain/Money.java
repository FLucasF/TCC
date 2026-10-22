package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Valores em reais, sempre com 2 casas e arredondamento "meio para o par". */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Escreve como R$ 1.234,56. */
    public static String brl(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.ROOT));
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        format.setDecimalFormatSymbols(symbols);
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + format.format(value);
    }
}
