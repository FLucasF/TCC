package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regra de dinheiro que vale para todas as etapas do resumo: centavos com
 * arredondamento meio para o par.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    static final MathContext PRECISAO = MathContext.DECIMAL128;

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre uma base, ja arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return arredondar(base.multiply(taxa));
    }
}
