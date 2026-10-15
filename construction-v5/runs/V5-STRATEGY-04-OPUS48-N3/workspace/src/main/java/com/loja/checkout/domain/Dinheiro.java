package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra única de dinheiro: tudo vira centavos com arredondamento "meio para o
 * par" (HALF_EVEN). 2,995 -> 3,00 e 2,985 -> 2,98.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredonda(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredonda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
