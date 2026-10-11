package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Arredondamento de dinheiro: 2 casas, meio para o par. */
public final class Money {

    private Money() {
    }

    /** Arredonda para centavos usando HALF_EVEN (ex.: 2,995 -> 3,00 e 2,985 -> 2,98). */
    public static BigDecimal centavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
