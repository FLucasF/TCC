package br.com.loja.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Aritmética de dinheiro: centavos, arredondamento "meio para o par". */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percent(BigDecimal base, String rate) {
        return round(base.multiply(new BigDecimal(rate)));
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }
}
