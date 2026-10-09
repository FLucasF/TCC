package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitário de arredondamento monetário.
 *
 * Toda etapa do cálculo usa o mesmo arredondamento: duas casas decimais,
 * "meio para o par" (HALF_EVEN). Ex.: 2,995 -> 3,00 e 2,985 -> 2,98.
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = valor(0);

    private Dinheiro() {
    }

    /** Arredonda para centavos usando HALF_EVEN. */
    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Constrói um valor já na escala de centavos. */
    public static BigDecimal valor(double v) {
        return arredondar(BigDecimal.valueOf(v));
    }
}
