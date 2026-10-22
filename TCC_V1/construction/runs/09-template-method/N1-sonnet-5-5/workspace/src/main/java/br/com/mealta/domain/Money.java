package br.com.mealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Regras de dinheiro: sempre 2 casas, arredondando meio para o par. */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** R$ 1.234,56 */
    public static String format(BigDecimal value) {
        return String.format(PT_BR, "R$ %,.2f", value);
    }

    /** 599.76 */
    public static String plain(BigDecimal value) {
        return round(value).toPlainString();
    }
}
