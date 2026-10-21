package pousada;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** Fraction of an amount (e.g. 0.15 of 100.00), rounded to cents. */
    public static BigDecimal fraction(BigDecimal amount, String fraction) {
        return round(amount.multiply(new BigDecimal(fraction)));
    }

    /** Brazilian format: R$ 1.234,56 */
    public static String format(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));
        return "R$ " + format.format(value);
    }
}
