package pousada;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Monetary helpers: every amount is rounded to cents, half-even, at each step. */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** "R$ 1.234,56" */
    public static String brl(BigDecimal value) {
        return "R$ " + format(value, '.', ',');
    }

    /** "BRL 1,234.56" */
    public static String brlEn(BigDecimal value) {
        return "BRL " + format(value, ',', '.');
    }

    private static String format(BigDecimal value, char grouping, char decimal) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(grouping);
        symbols.setDecimalSeparator(decimal);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return format.format(round(value));
    }
}
