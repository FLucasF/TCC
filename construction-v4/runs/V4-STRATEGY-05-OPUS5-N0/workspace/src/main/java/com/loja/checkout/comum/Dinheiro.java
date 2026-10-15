package com.loja.checkout.comum;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regras de arredondamento de dinheiro da loja: todo valor monetario fica com
 * duas casas decimais, arredondado "meio para o par" (HALF_EVEN).
 */
public final class Dinheiro {

    public static final BigDecimal ZERO = valor(BigDecimal.ZERO);

    /** Precisao usada nos calculos intermediarios (juros, percentuais). */
    public static final MathContext CALCULO = MathContext.DECIMAL128;

    private static final int CASAS = 2;

    private Dinheiro() {
    }

    /** Arredonda para centavos, meio para o par. */
    public static BigDecimal valor(BigDecimal bruto) {
        return bruto.setScale(CASAS, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre uma base, ja arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return valor(base.multiply(percentual, CALCULO).divide(new BigDecimal("100"), CALCULO));
    }
}
