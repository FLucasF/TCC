package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Todo valor em dinheiro e arredondado para centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Percentual sobre uma base, ja arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return centavos(base.multiply(taxa));
    }

    public static BigDecimal naoNegativo(BigDecimal valor) {
        return valor.signum() < 0 ? ZERO : valor;
    }
}
