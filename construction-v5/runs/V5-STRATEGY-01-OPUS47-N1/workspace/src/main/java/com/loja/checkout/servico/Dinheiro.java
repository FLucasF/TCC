package com.loja.checkout.servico;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class Dinheiro {
    public static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);

    private Dinheiro() {}

    public static BigDecimal arredondar(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
