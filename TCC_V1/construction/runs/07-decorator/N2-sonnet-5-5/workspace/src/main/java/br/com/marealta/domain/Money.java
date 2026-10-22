package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {}

    /** Arredondamento "meio para o par" em centavos, aplicado a cada passo do cálculo. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Escreve o valor como R$ 1.234,56. */
    public static String brl(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return "R$ " + new DecimalFormat("#,##0.00", symbols).format(round(value));
    }
}
