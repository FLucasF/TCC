package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de dinheiro: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre um valor, em centavos. 2.5 significa 2,5%. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxaPercentual) {
        return centavos(valor.multiply(taxaPercentual).divide(BigDecimal.valueOf(100)));
    }
}
