package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Arredondamento {
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Arredondamento() {}

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
