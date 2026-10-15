package com.loja.checkout;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class Dinheiro {
    public static final MathContext MC = new MathContext(20, RoundingMode.HALF_EVEN);
    public static final BigDecimal ZERO = emCentavos(BigDecimal.ZERO);

    private Dinheiro() {}

    public static BigDecimal emCentavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
