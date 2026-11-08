package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {
    }

    /** Arredonda para centavos, meio para o par. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** Fração (por exemplo 0.15) de um valor, já arredondada. */
    public static BigDecimal fraction(BigDecimal value, BigDecimal rate) {
        return round(value.multiply(rate));
    }

    /** R$ 1.234,56 */
    public static String format(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.of("pt", "BR")));
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + format.format(value);
    }
}
