package br.com.marealta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Money {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Money() {}

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** Arredonda para centavos, meio para o par. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** 599.76 */
    public static String csv(BigDecimal value) {
        return value.toPlainString();
    }

    /** R$ 1.234,56 */
    public static String brl(BigDecimal value) {
        return "R$ " + String.format(PT_BR, "%,.2f", value);
    }
}
