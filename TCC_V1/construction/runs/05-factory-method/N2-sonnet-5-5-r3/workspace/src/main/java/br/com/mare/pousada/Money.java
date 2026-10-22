package br.com.mare.pousada;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Aritmética de dinheiro: sempre centavos, arredondamento "meio para o par". */
public final class Money {
    public static final BigDecimal ZERO = round(BigDecimal.ZERO);

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percent(BigDecimal value, BigDecimal rate) {
        return round(value.multiply(rate));
    }
}
