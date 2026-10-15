package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Arredondamento {

    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Arredondamento() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
