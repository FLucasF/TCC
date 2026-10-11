package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {
    public static final BigDecimal ZERO = arredondar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal valor, String taxa) {
        return arredondar(valor.multiply(new BigDecimal(taxa)));
    }
}
