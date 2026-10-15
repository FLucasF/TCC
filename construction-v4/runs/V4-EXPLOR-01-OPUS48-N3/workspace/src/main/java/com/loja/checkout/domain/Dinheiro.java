package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de dinheiro, igual em todas as etapas: meio-para-o-par
 * (HALF_EVEN), sempre 2 casas decimais (centavos).
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
