package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centraliza a regra de dinheiro da loja: todo valor em reais e arredondado
 * para centavos usando "meio para o par" (HALF_EVEN). Exemplo: 2,995 -> 3,00
 * e 2,985 -> 2,98.
 */
public final class Dinheiro {

    public static final RoundingMode MODO = RoundingMode.HALF_EVEN;
    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    /** Arredonda um valor para centavos (2 casas), meio para o par. */
    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, MODO);
    }
}
