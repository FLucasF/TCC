package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class Money {
    private Money() {}

    /** Arredonda para centavos, meio para o par. */
    static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** R$ 1.234,56 */
    static String brl(BigDecimal v) {
        return "R$ " + format(v, '.', ',');
    }

    /** BRL 1,234.56 */
    static String en(BigDecimal v) {
        return "BRL " + format(v, ',', '.');
    }

    private static String format(BigDecimal v, char grouping, char decimal) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(grouping);
        symbols.setDecimalSeparator(decimal);
        DecimalFormat f = new DecimalFormat("#,##0.00", symbols);
        f.setRoundingMode(RoundingMode.HALF_EVEN);
        return f.format(v);
    }
}
