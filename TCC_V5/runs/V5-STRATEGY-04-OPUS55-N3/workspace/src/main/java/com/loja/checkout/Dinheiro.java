package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento único de valores em dinheiro: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual (ex.: 2.5 para 2,5%) sobre um valor, arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal valor, String percentual) {
        return centavos(valor.multiply(new BigDecimal(percentual)).movePointLeft(2));
    }
}
