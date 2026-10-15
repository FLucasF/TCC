package com.loja.resumo.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento "meio para o par" (HALF_EVEN) usado em toda etapa monetária do cálculo.
 */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
