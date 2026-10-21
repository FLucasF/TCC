package pousada;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** 1.234,56 */
    public static String brazilian(BigDecimal value) {
        return format(value, ',', '.');
    }

    /** 1,234.56 */
    public static String english(BigDecimal value) {
        return format(value, '.', ',');
    }

    private static String format(BigDecimal value, char decimal, char grouping) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(decimal);
        symbols.setGroupingSeparator(grouping);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return format.format(value);
    }
}
