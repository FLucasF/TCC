package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class Dinheiro {

    public static final BigDecimal ZERO = ajustar(BigDecimal.ZERO);
    public static final MathContext MC = new MathContext(20, RoundingMode.HALF_EVEN);

    private Dinheiro() {}

    public static BigDecimal ajustar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
