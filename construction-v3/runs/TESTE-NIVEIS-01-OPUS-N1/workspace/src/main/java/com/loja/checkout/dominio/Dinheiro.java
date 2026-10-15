package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regra de dinheiro que vale para todas as etapas do resumo: todo valor em
 * reais fica em centavos, arredondado meio para o par.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    /** Precisao das contas intermediarias, antes do arredondamento final. */
    public static final MathContext CONTAS = new MathContext(20, RoundingMode.HALF_EVEN);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre uma base, ja em centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return centavos(base.multiply(taxa));
    }
}
