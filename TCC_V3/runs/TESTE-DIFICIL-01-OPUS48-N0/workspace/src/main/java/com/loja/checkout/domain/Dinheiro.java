package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regras de dinheiro da loja: todo valor em reais e arredondado para centavos
 * usando "meio para o par" (HALF_EVEN), em cada etapa do calculo.
 */
public final class Dinheiro {

    public static final int ESCALA_CENTAVOS = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    /** Arredonda um valor para centavos (2 casas), meio para o par. */
    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(ESCALA_CENTAVOS, ARREDONDAMENTO);
    }

    /** Divide dividindo por um inteiro e arredonda o resultado para centavos. */
    public static BigDecimal dividirEmCentavos(BigDecimal valor, int divisor) {
        return valor.divide(BigDecimal.valueOf(divisor), ESCALA_CENTAVOS, ARREDONDAMENTO);
    }
}
