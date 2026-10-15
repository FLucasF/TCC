package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class Dinheiro {
    public static final MathContext MC = MathContext.DECIMAL64;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Dinheiro() {}

    public static BigDecimal arredondar(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
