package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Monetary helpers: every amount is rounded to cents, half-even. */
final class Money {
    static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    static BigDecimal of(String v) {
        return round(new BigDecimal(v));
    }

    /** R$ 1.234,56 */
    static String brl(BigDecimal v) {
        return "R$ " + format(v, ',', '.');
    }

    /** BRL 1,234.56 */
    static String brlEn(BigDecimal v) {
        return "BRL " + format(v, '.', ',');
    }

    /** 1234.56, for CSV. */
    static String plain(BigDecimal v) {
        return round(v).toPlainString();
    }

    private static String format(BigDecimal v, char decimal, char grouping) {
        DecimalFormatSymbols s = DecimalFormatSymbols.getInstance(Locale.ROOT);
        s.setDecimalSeparator(decimal);
        s.setGroupingSeparator(grouping);
        DecimalFormat f = new DecimalFormat("#,##0.00", s);
        f.setParseBigDecimal(true);
        return f.format(round(v));
    }
}
