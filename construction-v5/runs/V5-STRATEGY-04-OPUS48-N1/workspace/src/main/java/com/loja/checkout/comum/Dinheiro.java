package com.loja.checkout.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de dinheiro: igual em todas as etapas do cálculo.
 * Centavos com "meio para o par" (HALF_EVEN): 2,995 -> 3,00 e 2,985 -> 2,98.
 */
public final class Dinheiro {

    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, MODO);
    }

    public static BigDecimal zero() {
        return centavos(BigDecimal.ZERO);
    }
}
