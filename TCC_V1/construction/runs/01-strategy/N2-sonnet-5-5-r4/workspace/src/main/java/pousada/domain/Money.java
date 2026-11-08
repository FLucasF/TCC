package pousada.domain;

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

    /** Percentual como fração, ex.: percent(12) = 0.12. */
    public static BigDecimal percent(int value) {
        return BigDecimal.valueOf(value, 2);
    }

    public static BigDecimal portion(BigDecimal amount, BigDecimal fraction) {
        return round(amount.multiply(fraction));
    }

    /** Formato dos comprovantes: R$ 1.234,56. */
    public static String format(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(value);
    }
}
