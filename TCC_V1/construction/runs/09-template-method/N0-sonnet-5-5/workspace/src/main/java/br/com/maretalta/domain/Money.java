package br.com.maretalta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Dinheiro: sempre com 2 casas, arredondamento "meio para o par" a cada passo. */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String v) {
        return round(new BigDecimal(v));
    }

    /** 1234.56 -> "R$ 1.234,56" */
    public static String brl(BigDecimal v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setDecimalSeparator(',');
        s.setGroupingSeparator('.');
        return "R$ " + new DecimalFormat("#,##0.00", s).format(round(v));
    }

    /** 599.76 -> "599.76" (CSV) */
    public static String plain(BigDecimal v) {
        return round(v).toPlainString();
    }
}
