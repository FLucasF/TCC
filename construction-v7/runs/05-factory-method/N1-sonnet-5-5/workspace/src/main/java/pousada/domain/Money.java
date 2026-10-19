package pousada.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Arredondamento e formatação de dinheiro. */
public final class Money {

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
    public static String brlEnglish(BigDecimal value) {
        return "BRL " + format(value, '.', ',');
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
