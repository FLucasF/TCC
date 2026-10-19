package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Utilitários de dinheiro: tudo em BigDecimal com 2 casas, arredondamento meio para o par. */
public final class Money {
    private Money() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String v) {
        return round(new BigDecimal(v));
    }

    /** Formato brasileiro: R$ 1.234,56 */
    public static String brl(BigDecimal v) {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
        sym.setDecimalSeparator(',');
        sym.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", sym);
        df.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + df.format(round(v));
    }
}
