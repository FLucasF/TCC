package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de dinheiro: sempre centavos, "meio para o par" (HALF_EVEN).
 * Igual em todas as etapas do cálculo, por isso mora num lugar só.
 */
public final class Dinheiro {

    public static final int ESCALA = 2;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal zero() {
        return centavos(BigDecimal.ZERO);
    }
}
