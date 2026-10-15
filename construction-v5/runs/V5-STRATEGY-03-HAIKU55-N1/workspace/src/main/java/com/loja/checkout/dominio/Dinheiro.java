package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Dinheiro() {
    }

    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal base, String taxa) {
        return centavos(base.multiply(new BigDecimal(taxa)));
    }

    public static BigDecimal dividir(BigDecimal valor, int partes) {
        return valor.divide(BigDecimal.valueOf(partes), 2, RoundingMode.HALF_EVEN);
    }
}
