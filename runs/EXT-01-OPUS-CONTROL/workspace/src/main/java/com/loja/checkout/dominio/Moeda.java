package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento monetario do sistema: centavos, meio para o par. */
public final class Moeda {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Moeda() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return centavos(base.multiply(percentual));
    }
}
