package com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Dinheiro {

    static final BigDecimal ZERO = new BigDecimal("0.00");

    private Dinheiro() {
    }

    static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
