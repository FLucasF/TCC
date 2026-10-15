package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de dinheiro, igual em todas as etapas do calculo: centavos,
 * meio para o par (2,995 vira 3,00 e 2,985 vira 2,98).
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private static final int CASAS = 2;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(CASAS, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre um valor, ja arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal fracao) {
        return centavos(valor.multiply(fracao));
    }
}
