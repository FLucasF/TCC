package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regras de arredondamento de valores em dinheiro: sempre 2 casas decimais,
 * no modo "meio para o par" (2,995 -> 3,00 e 2,985 -> 2,98).
 */
public final class Dinheiro {

    /** Precisao usada nos calculos intermediarios, antes do arredondamento para centavos. */
    public static final MathContext PRECISAO = MathContext.DECIMAL64;

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    /** Arredonda para centavos, meio para o par. */
    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual (ex.: 2.5 para 2,5%) sobre uma base, arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return centavos(base.multiply(percentual).divide(BigDecimal.valueOf(100), PRECISAO));
    }
}
