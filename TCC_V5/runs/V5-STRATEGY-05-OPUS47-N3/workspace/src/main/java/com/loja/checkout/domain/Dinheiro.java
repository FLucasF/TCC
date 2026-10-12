package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {
    private Dinheiro() {}

    public static BigDecimal cent(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static final BigDecimal ZERO = cent(BigDecimal.ZERO);
}
