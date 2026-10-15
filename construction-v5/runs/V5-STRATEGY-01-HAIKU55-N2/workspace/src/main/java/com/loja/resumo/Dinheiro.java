package com.loja.resumo;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Dinheiro {

    static final BigDecimal ZERO = new BigDecimal("0.00");

    private Dinheiro() {
    }

    static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    static BigDecimal sobre(BigDecimal base, BigDecimal taxa) {
        return arredondar(base.multiply(taxa));
    }
}
