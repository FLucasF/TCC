package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regras de dinheiro da loja: todo valor monetario fica em centavos, arredondado
 * "meio para o par" em cada etapa do calculo.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    /** Precisao usada nas contas intermediarias, antes do arredondamento final. */
    public static final MathContext PRECISAO = MathContext.DECIMAL128;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return centavos(valor.multiply(taxa));
    }
}
