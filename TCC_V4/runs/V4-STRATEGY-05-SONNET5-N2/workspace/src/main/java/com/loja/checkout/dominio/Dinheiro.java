package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento monetário padrão da loja: meio para o par, 2 casas decimais. */
public final class Dinheiro {

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return arredondar(base.multiply(percentual));
    }
}
