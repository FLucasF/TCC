package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Arredondamento de valores em dinheiro: centavos, meio para o par.
 * Igual em todas as etapas do calculo.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = emCentavos(BigDecimal.ZERO);

    /** Precisao usada nas contas intermediarias, antes do arredondamento para centavos. */
    public static final MathContext PRECISAO = new MathContext(20, RoundingMode.HALF_EVEN);

    private static final int CASAS = 2;
    private static final BigDecimal CEM = new BigDecimal("100");

    private Dinheiro() {
    }

    public static BigDecimal emCentavos(BigDecimal valor) {
        return valor.setScale(CASAS, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal percentual, BigDecimal valor) {
        return emCentavos(valor.multiply(percentual).divide(CEM, PRECISAO));
    }
}
