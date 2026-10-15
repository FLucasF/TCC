package com.loja.checkout.domain.pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento monetario da loja: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = valor(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal valor(BigDecimal bruto) {
        return bruto.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal valor(String bruto) {
        return valor(new BigDecimal(bruto));
    }

    /** Percentual (ex.: 0.12) sobre uma base, ja arredondado para centavos. */
    public static BigDecimal percentual(BigDecimal base, BigDecimal taxa) {
        return valor(base.multiply(taxa));
    }
}
