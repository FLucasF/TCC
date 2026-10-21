package br.com.pousadamarealta.reservas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Dinheiro: sempre em centavos, arredondado "meio para o par" a cada passo. */
public final class Money {

    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** Formato do comprovante: R$ 1.234,56. */
    public static String format(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        return "R$ " + format.format(value);
    }
}
