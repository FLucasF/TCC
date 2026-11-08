package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Todo dinheiro é arredondado para centavos, meio para o par, a cada passo. */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String v) {
        return round(new BigDecimal(v));
    }

    public static BigDecimal percent(BigDecimal base, int pct) {
        return round(base.multiply(BigDecimal.valueOf(pct)).divide(BigDecimal.valueOf(100)));
    }

    /** R$ 1.234,56 */
    public static String brl(BigDecimal v) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
        sym.setDecimalSeparator(',');
        sym.setGroupingSeparator('.');
        DecimalFormat f = new DecimalFormat("#,##0.00", sym);
        f.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + f.format(round(v));
    }
}
