package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento monetário padrão do negócio: "meio para o par" (HALF_EVEN),
 * aplicado a cada etapa do cálculo do resumo.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
