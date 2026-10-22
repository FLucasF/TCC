package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Dinheiro: sempre em centavos, arredondamento "meio para o par". */
public final class Money {
    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal zero() {
        return round(BigDecimal.ZERO);
    }

    /** Aplica um percentual (ex.: 15 para 15%) e arredonda para centavos. */
    public static BigDecimal percentOf(BigDecimal value, int percent) {
        return round(value.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100)));
    }

    /** R$ 1.234,56 */
    public static String brazilian(BigDecimal value) {
        return "R$ " + format(value, '.', ',');
    }

    /** BRL 1,234.56 */
    public static String english(BigDecimal value) {
        return "BRL " + format(value, ',', '.');
    }

    private static String format(BigDecimal value, char grouping, char decimal) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(grouping);
        symbols.setDecimalSeparator(decimal);
        return new DecimalFormat("#,##0.00", symbols).format(round(value));
    }
}
