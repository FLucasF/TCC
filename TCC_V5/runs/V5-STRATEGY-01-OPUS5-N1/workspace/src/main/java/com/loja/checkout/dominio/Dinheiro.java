package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Arredondamento de dinheiro: centavos, meio para o par. */
public final class Dinheiro {

    public static final MathContext CONTA = MathContext.DECIMAL128;

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal valor, BigDecimal taxa) {
        return centavos(valor.multiply(taxa));
    }
}
