package com.loja.checkout.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de valores em dinheiro: centavos, "meio para o par". */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Aplica um percentual (ex.: 2.5 para 2,5%) e arredonda para centavos. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal percentual) {
        return centavos(valor.multiply(percentual).divide(CEM));
    }
}
