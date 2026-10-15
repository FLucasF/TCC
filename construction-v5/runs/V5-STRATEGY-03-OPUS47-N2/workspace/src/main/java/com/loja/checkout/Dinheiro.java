package com.loja.checkout;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class Dinheiro {
    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
    public static final MathContext MC = new MathContext(20, MODO);
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Dinheiro() {}

    public static BigDecimal arredonda(BigDecimal v) {
        return v.setScale(2, MODO);
    }
}
