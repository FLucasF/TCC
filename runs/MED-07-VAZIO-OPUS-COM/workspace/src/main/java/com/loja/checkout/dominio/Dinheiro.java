package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de valores monetarios: centavos, meio para o par. */
public final class Dinheiro {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Dinheiro() {
    }

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
