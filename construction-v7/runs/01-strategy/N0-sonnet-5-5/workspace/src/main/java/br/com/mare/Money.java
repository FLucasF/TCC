package br.com.mare;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Utilitários de dinheiro: sempre 2 casas, arredondamento meio para o par. */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String v) {
        return round(new BigDecimal(v));
    }

    /** Fração (ex.: 0.12) de um valor, já arredondada para centavos. */
    public static BigDecimal fraction(BigDecimal base, String fraction) {
        return round(base.multiply(new BigDecimal(fraction)));
    }

    /** Formato brasileiro: R$ 1.234,56 */
    public static String brl(BigDecimal v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setDecimalSeparator(',');
        s.setGroupingSeparator('.');
        return "R$ " + new DecimalFormat("#,##0.00", s).format(round(v));
    }
}
