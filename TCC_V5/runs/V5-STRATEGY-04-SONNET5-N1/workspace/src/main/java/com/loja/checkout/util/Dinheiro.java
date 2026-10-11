package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento monetário "meio para o par" (HALF_EVEN), usado em cada
 * etapa do cálculo do resumo da compra.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
