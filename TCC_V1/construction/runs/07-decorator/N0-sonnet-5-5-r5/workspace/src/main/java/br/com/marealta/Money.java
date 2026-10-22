package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {}

    /** Arredonda para centavos, meio para o par. */
    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** R$ 1.234,56 */
    public static String brl(BigDecimal v) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        f.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + f.format(v);
    }

    /** 599.76 */
    public static String plain(BigDecimal v) {
        return round(v).toPlainString();
    }
}
