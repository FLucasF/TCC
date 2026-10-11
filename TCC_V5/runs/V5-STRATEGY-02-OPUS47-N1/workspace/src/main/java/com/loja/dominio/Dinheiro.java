package com.loja.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Dinheiro() {}

    public static BigDecimal centavos(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
