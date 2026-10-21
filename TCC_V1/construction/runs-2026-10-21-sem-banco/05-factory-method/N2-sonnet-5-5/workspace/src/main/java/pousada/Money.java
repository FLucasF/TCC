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

    /** R$ 1.234,56 */
    public static String brazilian(BigDecimal value) {
        return "R$ " + format(value, ',', '.');
    }

    /** BRL 1,234.56 */
    public static String english(BigDecimal value) {
        return "BRL " + format(value, '.', ',');
    }

    /** 1234.56, como vai nos CSV */
    public static String plain(BigDecimal value) {
        return round(value).toPlainString();
    }

    private static String format(BigDecimal value, char decimal, char grouping) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setDecimalSeparator(decimal);
        symbols.setGroupingSeparator(grouping);
        return new DecimalFormat("#,##0.00", symbols).format(round(value));
    }
}
