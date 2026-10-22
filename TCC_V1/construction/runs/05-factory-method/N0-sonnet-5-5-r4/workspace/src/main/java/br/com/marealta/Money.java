package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Valores em reais, sempre com 2 casas e arredondamento "meio para o par". */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String v) {
        return round(new BigDecimal(v));
    }

    /** R$ 1.234,56 (sem o prefixo). */
    public static String pt(BigDecimal v) {
        return format(v, ',', '.');
    }

    /** 1,234.56 (sem o prefixo). */
    public static String en(BigDecimal v) {
        return format(v, '.', ',');
    }

    private static String format(BigDecimal v, char decimal, char grouping) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setDecimalSeparator(decimal);
        s.setGroupingSeparator(grouping);
        DecimalFormat f = new DecimalFormat("#,##0.00", s);
        f.setRoundingMode(RoundingMode.HALF_EVEN);
        return f.format(round(v));
    }
}
