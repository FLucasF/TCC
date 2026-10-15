package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regras de dinheiro da loja: todo valor monetario fica em centavos (2 casas)
 * e e arredondado "meio para o par" em cada etapa do calculo.
 */
public final class Moeda {

    public static final int CASAS = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    /** Precisao usada nas contas intermediarias (juros), antes do arredondamento final. */
    public static final MathContext PRECISAO_INTERNA = MathContext.DECIMAL128;

    public static final BigDecimal ZERO = emCentavos(BigDecimal.ZERO);

    private Moeda() {
    }

    /** Arredonda um valor para centavos, meio para o par. */
    public static BigDecimal emCentavos(BigDecimal valor) {
        return valor.setScale(CASAS, ARREDONDAMENTO);
    }

    /** Percentual sobre uma base, ja arredondado para centavos. {@code taxa} em fracao (0.05 = 5%). */
    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return emCentavos(base.multiply(taxa));
    }
}
