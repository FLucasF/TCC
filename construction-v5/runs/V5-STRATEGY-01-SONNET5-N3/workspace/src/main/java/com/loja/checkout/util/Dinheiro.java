package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento "meio para o par" (HALF_EVEN) em 2 casas, usado em todo valor monetário. */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
