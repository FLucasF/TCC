package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arredondamento de dinheiro. Todo valor em reais e arredondado para centavos
 * usando "meio para o par" (HALF_EVEN), como pede o enunciado. Essa regra e
 * igual em todas as etapas do calculo, por isso mora num lugar so.
 */
public final class Dinheiro {

    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, MODO);
    }
}
