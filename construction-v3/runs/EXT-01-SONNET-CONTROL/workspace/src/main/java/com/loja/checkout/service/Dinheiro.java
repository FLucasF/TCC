package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento monetario padrao da loja: "meio para o par" (half-even),
 * sempre com 2 casas decimais, aplicado em cada etapa do calculo.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
