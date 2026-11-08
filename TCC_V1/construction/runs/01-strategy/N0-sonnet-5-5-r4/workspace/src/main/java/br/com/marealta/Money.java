package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Aritmética de dinheiro: sempre 2 casas, arredondamento meio-para-o-par a cada passo. */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** base * fator, arredondado. */
    public static BigDecimal times(BigDecimal base, BigDecimal factor) {
        return round(base.multiply(factor));
    }

    /** "R$ 1.234,56" */
    public static String brl(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(round(value));
    }
}
