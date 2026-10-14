package com.loja.orders.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valores em reais: todo valor é arredondado para centavos com "meio para o par". */
public final class Money {

    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, ROUNDING);
    }

    /** Aplica uma taxa (ex.: 0.05 para 5%) e arredonda para centavos. */
    public static BigDecimal percentOf(BigDecimal base, BigDecimal rate) {
        return round(base.multiply(rate));
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
