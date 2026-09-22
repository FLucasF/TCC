package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento monetario padrao da loja: "meio para o par" (HALF_EVEN), 2 casas decimais.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
