package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Dinheiro sempre em centavos, arredondado "meio para o par". */
public final class Money {
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** R$ 1.234,56 */
    public static String brl(BigDecimal value) {
        return "R$ " + format(value, ',', '.');
    }

    /** BRL 1,234.56 */
    public static String international(BigDecimal value) {
        return "BRL " + format(value, '.', ',');
    }

    private static String format(BigDecimal value, char decimal, char grouping) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(decimal);
        symbols.setGroupingSeparator(grouping);
        return new DecimalFormat("#,##0.00", symbols).format(round(value));
    }
}
