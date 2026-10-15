package com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {
    private Dinheiro() {}

    public static BigDecimal cents(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal zero() {
        return cents(BigDecimal.ZERO);
    }
}
