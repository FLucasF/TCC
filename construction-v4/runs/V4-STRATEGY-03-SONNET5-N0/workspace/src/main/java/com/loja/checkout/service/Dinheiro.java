package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento "meio para o par" (banker's rounding) usado em toda etapa monetária.
 */
final class Dinheiro {

    private Dinheiro() {
    }

    static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
