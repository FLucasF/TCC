package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento monetário único usado em todas as etapas que produzem um valor em dinheiro:
 * "half to even" (banker's rounding), 2 casas decimais.
 */
public final class Arredondamento {

    private Arredondamento() {
    }

    public static BigDecimal monetario(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
