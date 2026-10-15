package com.loja.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Utilitário de arredondamento monetário: sempre 2 casas, meio-para-o-par. */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
