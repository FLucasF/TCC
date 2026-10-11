package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Arredondamento de valores em dinheiro: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = centavos(BigDecimal.ZERO);

    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre um valor, já arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal valor, BigDecimal percentual) {
        return centavos(valor.multiply(percentual).divide(BigDecimal.valueOf(100), PRECISAO));
    }

    public static BigDecimal dividir(BigDecimal valor, BigDecimal divisor) {
        return valor.divide(divisor, PRECISAO);
    }

    public static BigDecimal elevar(BigDecimal base, int expoente) {
        return base.pow(expoente, PRECISAO);
    }
}
