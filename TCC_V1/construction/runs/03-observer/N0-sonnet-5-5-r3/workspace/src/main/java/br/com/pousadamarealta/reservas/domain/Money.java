package br.com.pousadamarealta.reservas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Regras de arredondamento e formatação de valores em reais. */
public final class Money {
    public static final BigDecimal ZERO = scale(BigDecimal.ZERO);

    private static final DecimalFormatSymbols PT_BR = new DecimalFormatSymbols(Locale.of("pt", "BR"));

    private Money() {
    }

    /** Arredonda para centavos, "meio para o par". */
    public static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Ex.: R$ 1.234,56 */
    public static String format(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", PT_BR);
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + format.format(value);
    }
}
