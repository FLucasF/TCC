package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Único lugar que arredonda dinheiro: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredonda(BigDecimal.ZERO);

    private static final int CASAS = 2;

    private Dinheiro() {
    }

    public static BigDecimal arredonda(BigDecimal valor) {
        return valor.setScale(CASAS, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return arredonda(valor.multiply(taxa));
    }
}
