package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Arredondamento do dinheiro: centavos, meio para o par, em cada etapa. */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredonda(BigDecimal.ZERO);

    /** Precisao de trabalho das contas intermediarias (divisoes e potencias). */
    public static final MathContext CONTAS = MathContext.DECIMAL128;

    private static final int CENTAVOS = 2;

    private Dinheiro() {
    }

    public static BigDecimal arredonda(BigDecimal valor) {
        return valor.setScale(CENTAVOS, RoundingMode.HALF_EVEN);
    }

    /** Porcentagem sobre uma base, ja arredondada para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return arredonda(base.multiply(taxa));
    }
}
