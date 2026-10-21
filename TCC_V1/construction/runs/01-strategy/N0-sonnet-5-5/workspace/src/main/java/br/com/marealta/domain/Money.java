package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Aritmética monetária: centavos, arredondamento meio para o par a cada passo. */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percent(BigDecimal base, int percent) {
        return round(base.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100)));
    }

    /** R$ 1.234,56 */
    public static String brl(BigDecimal v) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
        sym.setGroupingSeparator('.');
        sym.setDecimalSeparator(',');
        return "R$ " + new DecimalFormat("#,##0.00", sym).format(round(v));
    }

    /** 599.76 */
    public static String plain(BigDecimal v) {
        return round(v).toPlainString();
    }
}
