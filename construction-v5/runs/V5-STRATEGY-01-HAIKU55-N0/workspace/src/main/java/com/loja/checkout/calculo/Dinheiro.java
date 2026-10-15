package com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Dinheiro {

    private Dinheiro() {
    }

    static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
