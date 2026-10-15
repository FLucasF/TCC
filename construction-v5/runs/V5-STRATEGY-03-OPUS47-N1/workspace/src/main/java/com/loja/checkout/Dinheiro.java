package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Dinheiro() {}

    public static BigDecimal centavos(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
