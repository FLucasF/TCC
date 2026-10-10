package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de valores em dinheiro: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre um valor, arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return centavos(valor.multiply(taxa));
    }
}
