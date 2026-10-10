package com.loja.checkout.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de dinheiro: centavos, meio para o par. Igual em todas as etapas. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre uma base, já arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return centavos(base.multiply(taxa));
    }
}
