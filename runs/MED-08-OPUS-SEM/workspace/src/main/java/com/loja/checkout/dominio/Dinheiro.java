package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Regras de arredondamento monetario da loja: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    /** Precisao usada nos calculos intermediarios (juros, percentuais). */
    public static final MathContext PRECISAO = new MathContext(24, RoundingMode.HALF_EVEN);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal de(String valor) {
        return new BigDecimal(valor);
    }
}
