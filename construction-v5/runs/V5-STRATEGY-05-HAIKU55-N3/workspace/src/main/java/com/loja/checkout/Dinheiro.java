package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Dinheiro {

    static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Dinheiro() {
    }

    static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
