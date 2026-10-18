package br.com.loja.pedidos.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {
    }

    /** Rounds to cents, half to even. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    /** value × rate, rounded to cents. */
    public static BigDecimal percent(BigDecimal value, BigDecimal rate) {
        return round(value.multiply(rate));
    }
}
