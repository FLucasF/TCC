package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Arredondamento e formatação de dinheiro, num lugar só. */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private static final DecimalFormatSymbols BR = new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR"));

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** "R$ 1.234,56" */
    public static String brl(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", BR);
        return "R$ " + format.format(round(value));
    }

    /** "599.76" */
    public static String plain(BigDecimal value) {
        return round(value).toPlainString();
    }
}
