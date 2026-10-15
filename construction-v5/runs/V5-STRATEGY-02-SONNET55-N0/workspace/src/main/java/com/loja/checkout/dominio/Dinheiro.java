package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de valores em reais: centavos, meio para o par. */
public final class Dinheiro {
    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return arredondar(base.multiply(taxa));
    }
}
