package br.com.pousadamarealta.reservas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Todo valor em dinheiro tem 2 casas, com arredondamento "meio para o par". */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Money() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }
}
