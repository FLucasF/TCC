package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Regras de arredondamento de dinheiro da loja: todo valor monetario fica com
 * duas casas decimais, arredondado "meio para o par" (2,995 -> 3,00; 2,985 -> 2,98).
 */
public final class Dinheiro {

    public static final int CASAS = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

    /** Precisao usada nos calculos intermediarios (juros, por exemplo). */
    public static final MathContext PRECISAO = new MathContext(34, RoundingMode.HALF_EVEN);

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(CASAS, ARREDONDAMENTO);
    }

    /** Percentual sobre um valor, ja arredondado para centavos. Ex.: percentual(409.70, 2.5) = 10.24. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal percentual) {
        return centavos(valor.multiply(percentual).divide(new BigDecimal("100"), PRECISAO));
    }
}
